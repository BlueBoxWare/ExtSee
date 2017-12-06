package com.gmail.blueboxware.extsee

import com.gmail.blueboxware.extsee.java.ExtSeeJavaExtensionTreeElement
import com.gmail.blueboxware.extsee.kotlin.ExtSeeKotlinExtensionTreeElement
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectAndLibrariesScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getTypeParameters
import org.jetbrains.kotlin.idea.refactoring.memberInfo.getClassDescriptorIfAny
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelExtensionsByReceiverTypeIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasByExpansionShortNameIndex
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.before
import org.jetbrains.kotlin.resolve.ModifiersChecker
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

/*
 * Copyright 2017 Blue Box Ware
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

private val acceptableVisibilities = listOf(Visibilities.PUBLIC, Visibilities.INTERNAL)

internal fun findExtensions(element: PsiElement, inherited: Boolean, doWhile: () -> Boolean): List<ExtSeeExtensionTreeElement> {

  val classDescriptor =
          when (element) {
            is KtClassOrObject -> element.descriptor as? ClassDescriptor
            is PsiClass -> element.getClassDescriptorIfAny()
            else -> null
          }

  val isJavaLangObject = element is PsiClass && element.qualifiedName == "java.lang.Object"

  return classDescriptor?.defaultType?.let { type ->
    getCallableTopLevelExtensions(element.project, type, inherited || isJavaLangObject, doWhile)
  }?.mapNotNull { callableDescriptor ->
    (callableDescriptor.findPsi() as? KtCallableDeclaration)?.let { psi ->
      if (element is KtClassOrObject) {
        ExtSeeKotlinExtensionTreeElement(psi, callableDescriptor, inherited)
      } else {
        ExtSeeJavaExtensionTreeElement(psi, callableDescriptor, inherited && !isJavaLangObject)
      }
    }
  } ?: listOf()

}

private fun getCallableTopLevelExtensions(
        project: Project,
        receiverType: KotlinType,
        isInherited: Boolean,
        doWhile: () -> Boolean
): Collection<CallableDescriptor> {

  val receiverTypeNames =
          if (isInherited) {
            receiverType.supertypes().flatMap { it.typeNames(project) }
          } else {
            receiverType.typeNames(project)
          }

  val index = KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE
  val scope = KotlinSourceFilterScope.sourcesAndLibraries(ProjectAndLibrariesScope(project), project)

  val declarations = index.getAllKeys(project)
          .filter {
            KotlinTopLevelExtensionsByReceiverTypeIndex.receiverTypeNameFromKey(it) in receiverTypeNames
          }
          .flatMap {
            index.get(it, project, scope)
          }.mapNotNull {
            it.navigationElement as? KtCallableDeclaration
          }.toSet()

  return findSuitableExtensions(declarations, receiverType, doWhile)

}

private fun findSuitableExtensions(
        declarations: Collection<KtCallableDeclaration>,
        receiverType: KotlinType,
        doWhile: () -> Boolean
): Collection<CallableDescriptor> {

  val result = mutableSetOf<CallableDescriptor>()

  fun processDescriptor(descriptor: CallableDescriptor) {
    if (descriptor.visibility in acceptableVisibilities && isApplicableTo(descriptor, receiverType)) {
      result.add(descriptor)
    }
  }

  declarations.toSet().filter {
    ModifiersChecker.resolveVisibilityFromModifiers(it, Visibilities.PUBLIC) in acceptableVisibilities
  }.flatMap {
    ProgressManager.checkCanceled()
    if (!doWhile()) {
      return listOf()
    }
    it.resolveToDescriptors()
  }.toSet().forEach {
    processDescriptor(it)
  }

  return result

}

private fun isApplicableTo(descriptor: CallableDescriptor, receiverType: KotlinType): Boolean =
  descriptor.fuzzyExtensionReceiverType()?.let { targetType ->
     receiverType.toFuzzyType(receiverType.getTypeParameters()).checkIsSuperTypeOf(targetType)?.substitution?.isEmpty() == false ||
            receiverType.toFuzzyType(receiverType.getTypeParameters()).checkIsSubtypeOf(targetType)?.substitution != null
  } ?: false


private fun KtCallableDeclaration.resolveToDescriptors(): Collection<CallableDescriptor> =
  fqName?.let { fqName ->
    val facade = getResolutionFacade()
    if (containingKtFile.isCompiled) {
      facade.resolveImportReference(facade.moduleDescriptor, fqName).filterIsInstance<CallableDescriptor>()
    } else {
      listOfNotNull(facade.resolveToDescriptor(this)).filterIsInstance<CallableDescriptor>()
    }
  } ?: listOf()

private fun KotlinType.typeNames(project: Project): Collection<String> {

  val typeNames = mutableSetOf<String>()

  constructor.declarationDescriptor?.name?.asString()?.let { typeName ->
    typeNames.add(typeName)
    resolveTypeAliasesUsingIndex(project, this, typeName).mapTo(typeNames, { it.name.asString() })
  }

  return typeNames

}

private fun resolveTypeAliasesUsingIndex(project: Project, type: KotlinType, originalTypeName: String): Set<TypeAliasDescriptor> {

  val typeConstructor = type.constructor

  val index = KotlinTypeAliasByExpansionShortNameIndex.INSTANCE
  val out = mutableSetOf<TypeAliasDescriptor>()

  fun searchRecursively(typeName: String) {
    index[typeName, project, ProjectAndLibrariesScope(project)].asSequence()
            .map { it.resolveToDescriptorIfAny() as? TypeAliasDescriptor }
            .filterNotNull()
            .filter { it.expandedType.constructor == typeConstructor }
            .filter { it !in out }
            .onEach { out.add(it) }
            .map { it.name.asString() }
            .forEach(::searchRecursively)
  }

  searchRecursively(originalTypeName)

  return out

}

internal fun NavigatablePsiElement.getLocationString(): String? {

  val containingFile = containingFile?.virtualFile ?: return null
  val index = ProjectFileIndex.getInstance(project)
  var suffix: String? = null
  if (index.isInLibrary(containingFile)) {
    suffix = index.getOrderEntriesForFile(containingFile).firstOrNull()?.presentableName
  } else {
    ModuleUtilCore.findModuleForFile(containingFile, project)?.let { module ->
      suffix = module.name
    }
  }

  return "in " + containingFile.presentableName + if (suffix != null) " [$suffix]" else ""

}

internal fun PsiElement.isInBody(): Boolean {
  PsiTreeUtil.findFirstParent(this, { it is KtClassBody || it is KtBlockExpression })?.let {
    return true
  }
  (PsiTreeUtil.findFirstParent(this, { it is PsiClass }) as? PsiClass)?.let { psiClass ->
    psiClass.lBrace?.let { lBrace ->
      psiClass.rBrace?.let { rBrace ->
        if (lBrace.before(this) && this.before(rBrace)) {
          return true
        }
      }
    }
  }
  return false
}