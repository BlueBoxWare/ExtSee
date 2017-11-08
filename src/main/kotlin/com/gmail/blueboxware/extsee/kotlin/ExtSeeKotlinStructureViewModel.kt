package com.gmail.blueboxware.extsee.kotlin

import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.structureView.KotlinInheritedMembersNodeProvider
import org.jetbrains.kotlin.idea.structureView.KotlinStructureViewElement
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.contains


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
class ExtSeeKotlinStructureViewModel(ktFile: KtFile): StructureViewModelBase(ktFile, KotlinStructureViewElement(ktFile, false)) {

  init {
    withSuitableClasses(KtDeclaration::class.java)
    withSorters(Sorter.ALPHA_SORTER)
  }

  private val extSeeExtensions = mutableSetOf<KotlinStructureViewElement>()

  private val myNodeProviders =
          NODE_PROVIDERS +
                  ExtSeeKotlinExtensionsNodeProvider(this) +
                  ExtSeeKotlinInheritedExtensionsNodeProvider(this)

  private val myFilters: Array<Filter> = arrayOf(PublicElementsFilter(this))

  fun addExtSeeExtension(kotlinStructureViewElement: KotlinStructureViewElement) =
          extSeeExtensions.add(kotlinStructureViewElement)

  fun isExtSeeExtension(kotlinStructureViewElement: KotlinStructureViewElement): Boolean =
          extSeeExtensions.contains(kotlinStructureViewElement)

  override fun getNodeProviders(): Collection<NodeProvider<TreeElement>> = myNodeProviders

  override fun getFilters(): Array<Filter> = myFilters

  private class PublicElementsFilter(val model: ExtSeeKotlinStructureViewModel): Filter {
    override fun isReverted(): Boolean = true

    override fun getPresentation(): ActionPresentation =
            ActionPresentationData("Show non-public", null, PlatformIcons.PRIVATE_ICON)

    override fun getName(): String = "KOTLIN_SHOW_NON_PUBLIC"

    override fun isVisible(treeNode: TreeElement?): Boolean {

      if (treeNode !is KotlinStructureViewElement) {
        return true
      }

      if (model.isExtSeeExtension(treeNode)) {
        (treeNode.element as? KtCallableDeclaration)?.let { ktCallableDeclaration ->
          (ktCallableDeclaration.descriptor as? DeclarationDescriptorWithVisibility)?.let { descriptor ->
            if (descriptor.visibility == Visibilities.PUBLIC) {
              return true
            } else if (descriptor.visibility == Visibilities.INTERNAL) {
              return GlobalSearchScope.projectScope(ktCallableDeclaration.project).contains(ktCallableDeclaration)
            }
          }
        }
        return false
      }

      return treeNode.isPublic
    }
  }

  companion object {
    val NODE_PROVIDERS: Collection<NodeProvider<TreeElement>> = listOf(KotlinInheritedMembersNodeProvider())
  }

}