package com.gmail.blueboxware.extsee.kotlin

import com.gmail.blueboxware.extsee.findExtensions
import com.intellij.icons.AllIcons
import com.intellij.ide.util.ActionShortcutProvider
import com.intellij.ide.util.FileStructureNodeProvider
import com.intellij.ide.util.treeView.smartTree.ActionPresentation
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.ui.LayeredIcon
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.structureView.KotlinStructureViewElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import javax.swing.Icon


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
class ExtSeeKotlinInheritedExtensionsNodeProvider: FileStructureNodeProvider<TreeElement>, ActionShortcutProvider {

  override fun provideNodes(node: TreeElement): Collection<TreeElement> {

    if (node !is KotlinStructureViewElement) {
      return listOf()
    }

    val ktClass = node.element as? KtClassOrObject ?: return listOf()

    return findExtensions(ktClass, true)

  }

  override fun getCheckBoxText(): String = "Show inherited extensions"

  override fun getShortcut(): Array<Shortcut> = arrayOf()

  override fun getPresentation(): ActionPresentation = ActionPresentationData(
          "Show inherited extensions", null, ICON
  )

  override fun getName(): String = "SHOW_INHERITED_EXTENSIONS"

  override fun getActionIdForShortcut(): String = "FileStructurePopup"

  companion object {
    val ICON: Icon = LayeredIcon.create(KotlinIcons.LAMBDA, AllIcons.Javaee.InheritedAttributeOverlay)
  }

}