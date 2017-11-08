package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.impl.java.AccessLevelProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectAndLibrariesScope
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getTypeParameters
import org.jetbrains.kotlin.idea.refactoring.memberInfo.getClassDescriptorIfAny
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.structureView.KotlinStructureViewElement
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelExtensionsByReceiverTypeIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasByExpansionShortNameIndex
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.*
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
internal fun findExtensions(element: PsiElement, inherited: Boolean): List<KotlinStructureViewElement> {

  val classDescriptor =
          when (element) {
            is KtClassOrObject -> element.descriptor as? ClassDescriptor
            is PsiClass -> element.getClassDescriptorIfAny()
            else -> null
          }

  return classDescriptor?.defaultType?.let { type ->
    getCallableTopLevelExtensions(element.project, type, inherited)
  }?.mapNotNull {
    (it.findPsi() as? KtElement)?.let {
      KotlinStructureViewElement(it, inherited)
    }
  } ?: listOf()


}


private val acceptableVisibilities = listOf(Visibilities.PUBLIC, Visibilities.INTERNAL)

internal fun getDescriptor(element: PsiElement): CallableDescriptor? {

  if (!(element.isValid && element is KtDeclaration)) {
    return null
  }

  if (element is KtAnonymousInitializer) {
    return null
  }

  return ApplicationManager.getApplication().runReadAction(
          Computable<CallableDescriptor> {
            if (!DumbService.isDumb(element.project)) {
              return@Computable (element as? KtDeclaration)?.resolveToDescriptor() as? CallableDescriptor
            }
            null
          }
  )

}

internal fun getAccessLevel(element: Any?): Int {
  if (element is AccessLevelProvider) {
    return element.accessLevel
  } else if (element is KotlinStructureViewElement) {
    ((element.element as? KtNamedFunction)?.descriptor as? DeclarationDescriptorWithVisibility)?.visibility?.let { visibility ->
      if (visibility == Visibilities.PUBLIC) {
        return PsiUtil.ACCESS_LEVEL_PUBLIC
      } else if (visibility == Visibilities.INTERNAL) {
        return PsiUtil.ACCESS_LEVEL_PACKAGE_LOCAL
      }
    }
  }
  return -1
}

//
// org.jetbrains.kotlin.idea.core.KotlinIndicesHelper::getCallableTopLevelExtensions()
//
private fun getCallableTopLevelExtensions(
        project: Project,
        receiverType: KotlinType,
        isInherited: Boolean
): Collection<CallableDescriptor> {

  val receiverTypeNames =
          if (isInherited) {
            receiverType.supertypes().flatMap { it.typeNames(project) }
          } else {
            receiverType.typeNames(project)
          }

  val index = KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE
  val scope = KotlinSourceFilterScope.projectAndLibrariesSources(ProjectAndLibrariesScope(project), project)

  val declarations = index.getAllKeys(project)
          .asSequence()
          .filter {
            ProgressManager.checkCanceled()
            KotlinTopLevelExtensionsByReceiverTypeIndex.receiverTypeNameFromKey(it) in receiverTypeNames
          }
          .flatMap {
            index.get(it, project, scope).asSequence()
          }

  return findSuitableExtensions(declarations, receiverType)

}

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
    ProgressManager.checkCanceled()
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

private fun findSuitableExtensions(
        declarations: Sequence<KtCallableDeclaration>,
        receiverType: KotlinType
): Collection<CallableDescriptor> {

  val result = mutableSetOf<CallableDescriptor>()

  fun processDescriptor(descriptor: CallableDescriptor) {
    if (isApplicableTo(descriptor, receiverType) && descriptor.visibility in acceptableVisibilities) {
      result.add(descriptor)
    }
  }

  declarations.forEach { it.resolveToDescriptors().forEach(::processDescriptor) }

  return result

}

private fun isApplicableTo(descriptor: CallableDescriptor, receiverType: KotlinType): Boolean {

  descriptor.fuzzyExtensionReceiverType()?.let { targetType ->
    return receiverType.toFuzzyType(receiverType.getTypeParameters()).checkIsSuperTypeOf(targetType)?.substitution?.isEmpty() == false ||
            receiverType.toFuzzyType(receiverType.getTypeParameters()).checkIsSubtypeOf(targetType)?.substitution != null
  }

  return false
}

private fun KtCallableDeclaration.resolveToDescriptors(): Collection<CallableDescriptor> {
  fqName?.let { fqName ->
    val facade = getResolutionFacade()
    return if (containingKtFile.isCompiled) {
      facade.resolveImportReference(facade.moduleDescriptor, fqName).filterIsInstance<CallableDescriptor>()
    } else {
      listOfNotNull(facade.resolveToDescriptor(this)).filterIsInstance<CallableDescriptor>()
    }
  }
  return listOf()
}

internal fun KotlinStructureViewElement.getLocationString(): String? {

  val project = element.project
  val containingFile = element.containingFile?.virtualFile ?: return null
  val index = ProjectFileIndex.getInstance(project)
  var suffix: String? = null
  if (index.isInLibrary(containingFile)) {
    suffix = index.getOrderEntriesForFile(containingFile).firstOrNull()?.presentableName
    //root = (DirectoryIndex.getInstance(project).getInfoForFile(containingFile) as? DirectoryInfoImpl)?.root
  } else {
    ModuleUtilCore.findModuleForFile(containingFile, project)?.let { module ->
      suffix = module.name
    }
    // root = index.getContentRootForFile(containingFile)
  }

  return " in " + containingFile.presentableName + if (suffix != null) " [$suffix]" else ""

}
