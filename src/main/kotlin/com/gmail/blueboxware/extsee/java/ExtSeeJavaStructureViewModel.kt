package com.gmail.blueboxware.extsee.java

import com.gmail.blueboxware.extsee.ExtSeeExtensionTreeElement
import com.gmail.blueboxware.extsee.ExtSeeStructureViewModel
import com.gmail.blueboxware.extsee.ExtensionsCollector
import com.intellij.ide.structureView.StructureView
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.java.*
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
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
internal class ExtSeeJavaStructureViewModel(
        psiJavaFile: PsiJavaFile,
        editor: Editor?
): JavaFileTreeModel(psiJavaFile, editor), ExtSeeStructureViewModel {

  override var structureView: StructureView? = null

  private val extensionsCollector = ExtensionsCollector(psiJavaFile.project, this)

  override fun getNodeProviders(): Collection<NodeProvider<TreeElement>> =
    super.getNodeProviders() +
            ExtSeeJavaExtensionsNodeProvider(extensionsCollector) +
            ExtSeeJavaInheritedExtensionsNodeProvider(extensionsCollector)


  override fun getFilters(): Array<Filter> = FILTERS

  override fun getSorters(): Array<Sorter> = arrayOf<Sorter>(
          if (TreeStructureUtil.isInStructureViewPopup(this)) {
            ExtSeeKindSorter.POPUP_INSTANCE
          } else {
            ExtSeeKindSorter.INSTANCE
          }
  ) + SORTERS

  override fun getGroupers(): Array<Grouper> =
          arrayOf(ExtSeeSuperTypesGrouper(), PropertiesGrouper())

  override fun shouldEnterElement(element: Any?): Boolean = element !is ExtSeeExtensionTreeElement

  override fun dispose() {
    super.dispose()
    Disposer.dispose(extensionsCollector)
  }

  companion object {

    val FILTERS = arrayOf(
            FieldsFilter(),
            object : PublicElementsFilter() {
              override fun isVisible(treeNode: TreeElement?): Boolean {
                if (treeNode is ExtSeeExtensionTreeElement) {
                  if (treeNode.accessLevel == PsiUtil.ACCESS_LEVEL_PUBLIC) {
                    return true
                  } else if (treeNode.accessLevel == PsiUtil.ACCESS_LEVEL_PACKAGE_LOCAL) {
                    val element = treeNode.callableDeclaration
                    return GlobalSearchScope.projectScope(element.project).contains(element)
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

              fun accessLevel(o: Any) =
                      (o as? ExtSeeExtensionTreeElement)?.accessLevel ?: ((o as? StructureViewTreeElement)?.value as? AccessLevelProvider)?.accessLevel ?: -1

              accessLevel(o2) - accessLevel(o1)
            }

  }

}
