package com.gmail.blueboxware.extsee.kotlin

import com.gmail.blueboxware.extsee.ExtSeeExtensionTreeElement
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.structureView.KotlinInheritedMembersNodeProvider
import org.jetbrains.kotlin.idea.structureView.KotlinStructureViewElement
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
  }

  override fun getNodeProviders(): Collection<NodeProvider<TreeElement>> = NODE_PROVIDERS

  override fun getFilters(): Array<Filter> = FILTERS

  override fun getSorters(): Array<Sorter> = SORTERS

  companion object {
    private val NODE_PROVIDERS: Collection<NodeProvider<TreeElement>> = listOf(
            KotlinInheritedMembersNodeProvider(),
            ExtSeeKotlinExtensionsNodeProvider(),
            ExtSeeKotlinInheritedExtensionsNodeProvider()
    )

    private val SORTERS: Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

    private val FILTERS: Array<Filter> = arrayOf(PublicElementsFilter())

    private class PublicElementsFilter: Filter {
      override fun isReverted(): Boolean = true

      override fun getPresentation(): ActionPresentation =
              ActionPresentationData("Show non-public", null, PlatformIcons.PRIVATE_ICON)

      override fun getName(): String = "KOTLIN_SHOW_NON_PUBLIC"

      override fun isVisible(treeNode: TreeElement?): Boolean {

        if (treeNode is ExtSeeExtensionTreeElement) {
          (treeNode.callableDescriptor as? DeclarationDescriptorWithVisibility)?.let { descriptor ->
            if (descriptor.visibility == Visibilities.PUBLIC) {
              return true
            } else if (descriptor.visibility == Visibilities.INTERNAL) {
              val element = treeNode.navigationElement
              return GlobalSearchScope.projectScope(element.project).contains(element)
            }
          }
          return false
        } else if (treeNode is KotlinStructureViewElement) {
          return treeNode.isPublic
        }

        return true
      }

    }

  }

}