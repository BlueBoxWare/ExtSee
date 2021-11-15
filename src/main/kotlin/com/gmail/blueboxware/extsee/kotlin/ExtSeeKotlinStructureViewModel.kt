package com.gmail.blueboxware.extsee.kotlin

import com.gmail.blueboxware.extsee.ExtSeeExtensionTreeElement
import com.gmail.blueboxware.extsee.ExtSeeStructureViewModel
import com.gmail.blueboxware.extsee.ExtensionsCollector
import com.intellij.ide.structureView.StructureView
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
import com.intellij.util.PlatformIcons
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
internal class ExtSeeKotlinStructureViewModel(ktFile: KtFile):
  StructureViewModelBase(ktFile, KotlinStructureViewElement(ktFile, false)),
  StructureViewModel.ElementInfoProvider,
  ExtSeeStructureViewModel {

  override var structureView: StructureView? = null

  private val extensionsCollector = ExtensionsCollector(ktFile.project, this)

  init {
    withSuitableClasses(KtDeclaration::class.java)
  }

  override fun getNodeProviders(): Collection<NodeProvider<TreeElement>> =
    NODE_PROVIDERS +
            ExtSeeKotlinExtensionsNodeProvider(extensionsCollector) +
            ExtSeeKotlinInheritedExtensionsNodeProvider(extensionsCollector)


  override fun getFilters(): Array<Filter> = FILTERS

  override fun getSorters(): Array<Sorter> = SORTERS

  override fun isAlwaysShowsPlus(element: StructureViewTreeElement?): Boolean = false

  override fun isAlwaysLeaf(element: StructureViewTreeElement?): Boolean = element is ExtSeeExtensionTreeElement

  override fun shouldEnterElement(element: Any?): Boolean = element !is ExtSeeExtensionTreeElement

  override fun dispose() {
    extensionsCollector.dispose()
    super.dispose()
  }

  companion object {
    private val NODE_PROVIDERS: Collection<NodeProvider<TreeElement>> = listOf(
      KotlinInheritedMembersNodeProvider()
    )

    private val SORTERS: Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

    private val FILTERS: Array<Filter> = arrayOf(PublicElementsFilter())

    private class PublicElementsFilter: Filter {
      override fun isReverted(): Boolean = true

      override fun getPresentation(): ActionPresentation =
        ActionPresentationData("Show Non-Public", null, PlatformIcons.PRIVATE_ICON)

      override fun getName(): String = "KOTLIN_SHOW_NON_PUBLIC"

      override fun isVisible(treeNode: TreeElement?): Boolean {

        if (treeNode is ExtSeeExtensionTreeElement) {
          if (treeNode.accessLevel == PsiUtil.ACCESS_LEVEL_PUBLIC) {
            return true
          } else if (treeNode.accessLevel == PsiUtil.ACCESS_LEVEL_PACKAGE_LOCAL) {
            val element = treeNode.callableDeclaration
            return GlobalSearchScope.projectScope(element.project).contains(element)
          }
          return false
        } else if (treeNode is KotlinStructureViewElement) {
          return treeNode.visibility.isPublic
        }

        return true
      }

    }

  }

}
