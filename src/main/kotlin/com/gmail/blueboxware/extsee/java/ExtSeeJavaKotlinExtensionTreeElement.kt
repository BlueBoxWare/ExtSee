package com.gmail.blueboxware.extsee.java

import com.gmail.blueboxware.extsee.getLocationString
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ColoredItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.LayeredIcon
import com.intellij.util.IconUtil
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.structureView.KotlinStructureViewElement
import javax.swing.Icon
import javax.swing.SwingConstants


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
class ExtSeeJavaKotlinExtensionTreeElement(private val kotlinStructureViewTreeElement: KotlinStructureViewElement): StructureViewTreeElement {

  fun isInherited(): Boolean = kotlinStructureViewTreeElement.isInherited

  override fun getPresentation(): ColoredItemPresentation = object : ColoredItemPresentation {
    override fun getLocationString(): String? = kotlinStructureViewTreeElement.getLocationString()

    override fun getIcon(unused: Boolean): Icon? = ICON

    override fun getPresentableText(): String? = kotlinStructureViewTreeElement.presentation.presentableText

    override fun getTextAttributesKey(): TextAttributesKey? = (kotlinStructureViewTreeElement.presentation as? ColoredItemPresentation)?.textAttributesKey
  }

  override fun navigate(requestFocus: Boolean) = kotlinStructureViewTreeElement.navigate(requestFocus)

  override fun getChildren(): Array<TreeElement> = kotlinStructureViewTreeElement.children

  override fun canNavigate(): Boolean = kotlinStructureViewTreeElement.canNavigate()

  override fun getValue(): Any = kotlinStructureViewTreeElement.value

  override fun canNavigateToSource(): Boolean = kotlinStructureViewTreeElement.canNavigateToSource()

  companion object {
    val ICON = LayeredIcon(2).apply {
      setIcon(KotlinIcons.LAMBDA, 0)
      setIcon(IconUtil.scale(KotlinIcons.SMALL_LOGO, .5), 1, SwingConstants.SOUTH_EAST)
    }
  }

}