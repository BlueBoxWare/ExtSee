package com.gmail.blueboxware.extsee.java

import com.gmail.blueboxware.extsee.ExtSeeExtensionTreeElement
import com.gmail.blueboxware.extsee.getAccessLevel
import com.intellij.ide.structureView.impl.java.*
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.psi.psiUtil.contains
import java.util.*


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
class ExtSeeJavaStructureViewModel(
        psiJavaFile: PsiJavaFile,
        editor: Editor?
): JavaFileTreeModel(psiJavaFile, editor) {

  override fun getNodeProviders(): Collection<NodeProvider<TreeElement>> =
    super.getNodeProviders() + NODE_PROVIDERS

  override fun getFilters(): Array<Filter> = FILTERS

  override fun getSorters(): Array<Sorter> = arrayOf(
          if (TreeStructureUtil.isInStructureViewPopup(this)) {
            KindSorter.POPUP_INSTANCE
          } else {
            KindSorter.INSTANCE
          }
  ) + SORTERS

  override fun getGroupers(): Array<Grouper> =
          arrayOf(ExtSeeSuperTypesGrouper(), PropertiesGrouper())

  companion object {
    val NODE_PROVIDERS = listOf<NodeProvider<TreeElement>>(
            ExtSeeJavaExtensionsNodeProvider(),
            ExtSeeJavaInheritedExtensionsNodeProvider()
    )

    val FILTERS = arrayOf(
            FieldsFilter(),
            object : PublicElementsFilter() {
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
                }
                return super.isVisible(treeNode)
              }
            }
    )

    val SORTERS = arrayOf(
            object: VisibilitySorter() {
              override fun getComparator(): Comparator<*> = visibilityComparator
            },
            AnonymousClassesSorter.INSTANCE,
            Sorter.ALPHA_SORTER
    )

    val visibilityComparator =
            Comparator<Any> { o1, o2 ->
              getAccessLevel(o2, (o2 as? ExtSeeExtensionTreeElement)?.callableDescriptor)
              - getAccessLevel(o1, (o1 as? ExtSeeExtensionTreeElement)?.callableDescriptor)
            }

  }

}
