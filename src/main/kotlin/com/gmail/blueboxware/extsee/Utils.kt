package com.gmail.blueboxware.extsee

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.javaResolutionFacade
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getTypeParameters
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelExtensionsByReceiverTypeIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasByExpansionShortNameIndex
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.before
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
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

@Suppress("unused", "PropertyName")
internal val LOGGER = Logger.getInstance("#com.gmail.blueboxware.Extsee")

private val acceptableVisibilities = listOf(PsiUtil.ACCESS_LEVEL_PUBLIC, PsiUtil.ACCESS_LEVEL_PACKAGE_LOCAL)

internal fun findExtensions(
  element: PsiElement,
  inherited: Boolean,
  doWhile: () -> Boolean
): List<ExtSeeExtensionTreeElement> {

  val classDescriptor =
    when (element) {
      is KtClassOrObject -> {
        element.descriptor as? ClassDescriptor
      }
      is PsiClass -> {
        try {
          element.javaResolutionFacade()?.let { element.getJavaClassDescriptor(it) }
        } catch (e: AssertionError) {
          null
        }
      }
      else -> {
        null
      }
    }

  val isJavaLangObject = element is PsiClass && element.qualifiedName == "java.lang.Object"

  val project = element.project
  val virtualFile = element.containingFile?.let { it.virtualFile ?: it.originalFile.virtualFile } ?: return emptyList()
  val projectFileIndex = ProjectFileIndex.getInstance(project)

  val modules = if (projectFileIndex.isInLibrary(virtualFile)) {
    projectFileIndex.getOrderEntriesForFile(virtualFile).map { orderEntry ->
      orderEntry.ownerModule
    }
  } else {
    element.module?.let {
      listOf(it)
    } ?: emptyList()
  }

  if (modules.isEmpty()) {
    return emptyList()
  }

  val delegateScope = GlobalSearchScope.union(
    modules.map { module ->
      GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        .uniteWith(GlobalSearchScope.moduleWithDependentsScope(module))
    }.toTypedArray()
  )

  val scope = KotlinSourceFilterScope.projectSourceAndClassFiles(delegateScope, project)

  return classDescriptor?.defaultType?.let { type ->
    getCallableTopLevelExtensions(
      element.project,
      type,
      inherited || isJavaLangObject,
      scope,
      doWhile,
      if (element is KtClassOrObject) KotlinFileType.INSTANCE else JavaFileType.INSTANCE
    )
  } ?: listOf()

}

private fun getCallableTopLevelExtensions(
  project: Project,
  receiverType: KotlinType,
  isInherited: Boolean,
  scope: GlobalSearchScope,
  doWhile: () -> Boolean,
  fileType: FileType
): List<ExtSeeExtensionTreeElement> {

  val receiverTypeNames =
    if (isInherited) {
      receiverType.supertypes().flatMap { it.typeNames(project, scope) }
    } else {
      receiverType.typeNames(project, scope)
    }

  val index = KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE

  val declarations = index.getAllKeys(project)
    .filter {
      KotlinTopLevelExtensionsByReceiverTypeIndex.receiverTypeNameFromKey(it) in receiverTypeNames
    }
    .flatMap {
      ProgressManager.checkCanceled()
      if (doWhile()) {
        index.get(it, project, scope)
      } else {
        listOf()
      }
    }.mapNotNull {
      it.navigationElement as? KtCallableDeclaration
    }.toSet()

  return findSuitableExtensions(declarations, receiverType, doWhile, isInherited, fileType)

}

private fun findSuitableExtensions(
  declarations: Collection<KtCallableDeclaration>,
  receiverType: KotlinType,
  doWhile: () -> Boolean,
  isInherited: Boolean,
  fileType: FileType
): List<ExtSeeExtensionTreeElement> {

  val result = mutableListOf<ExtSeeExtensionTreeElement>()

  declarations.toSet().filter {
    it.visibility() in acceptableVisibilities
  }.forEach { declaration ->
    ProgressManager.checkCanceled()
    if (doWhile()) {
      (declaration.resolveToDescriptorIfAny(BodyResolveMode.PARTIAL) as? CallableDescriptor)?.let { descriptor ->
        if (declaration.visibility() in acceptableVisibilities && isApplicableTo(descriptor, receiverType)) {
          result.add(ExtSeeExtensionTreeElement(declaration, descriptor, isInherited, fileType))
        }
      }
    }
  }

  return result

}

private fun isApplicableTo(descriptor: CallableDescriptor, receiverType: KotlinType): Boolean =
  descriptor.fuzzyExtensionReceiverType()?.let { targetType ->
    receiverType.toFuzzyType(receiverType.getTypeParameters())
      .checkIsSuperTypeOf(targetType)?.substitution?.isEmpty() == false ||
            receiverType.toFuzzyType(receiverType.getTypeParameters())
              .checkIsSubtypeOf(targetType)?.substitution != null
  } ?: false

private fun KotlinType.typeNames(project: Project, scope: GlobalSearchScope): Collection<String> {

  val typeNames = mutableSetOf<String>()

  constructor.declarationDescriptor?.name?.asString()?.let { typeName ->
    typeNames.add(typeName)
    resolveTypeAliasesUsingIndex(project, this, scope, typeName).mapTo(typeNames) { it.name.asString() }
  }

  return typeNames

}

private fun resolveTypeAliasesUsingIndex(
  project: Project,
  type: KotlinType,
  scope: GlobalSearchScope,
  originalTypeName: String
): Set<TypeAliasDescriptor> {

  val typeConstructor = type.constructor

  val index = KotlinTypeAliasByExpansionShortNameIndex.INSTANCE
  val out = mutableSetOf<TypeAliasDescriptor>()

  fun searchRecursively(typeName: String) {
    ProgressManager.checkCanceled()
    index[typeName, project, scope].asSequence()
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

  val virtualFile = containingFile?.virtualFile ?: return null
  val index = ProjectFileIndex.getInstance(project)

  val source = if (index.isInLibrary(virtualFile)) {
    index.getOrderEntriesForFile(virtualFile).firstOrNull()?.presentableName?.let {
      "from [$it]"
    }
  } else {
    if (project.allModules().size > 1) {
      ModuleUtilCore.findModuleForFile(virtualFile, project)?.let { module ->
        "from [$module]"
      }
    } else null
  }

  var location = virtualFile.nameWithoutExtension
  (containingFile as? KtFile)?.packageFqName?.asString()?.let { packageFqName ->
    if (packageFqName != "") {
      location = "$packageFqName.$location"
    }
  }

  return "in $location $source"

}

internal fun PsiElement.isInBody(): Boolean {
  PsiTreeUtil.findFirstParent(this) { it is KtClassBody || it is KtBlockExpression }?.let {
    return true
  }
  (PsiTreeUtil.findFirstParent(this) { it is PsiClass } as? PsiClass)?.let { psiClass ->
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

internal fun KtModifierListOwner.visibility(): Int =
  when {
    modifierList?.hasModifier(PRIVATE_KEYWORD) == true -> PsiUtil.ACCESS_LEVEL_PRIVATE
    modifierList?.hasModifier(PROTECTED_KEYWORD) == true -> PsiUtil.ACCESS_LEVEL_PROTECTED
    modifierList?.hasModifier(INTERNAL_KEYWORD) == true -> PsiUtil.ACCESS_LEVEL_PACKAGE_LOCAL
    else -> PsiUtil.ACCESS_LEVEL_PUBLIC
  }
