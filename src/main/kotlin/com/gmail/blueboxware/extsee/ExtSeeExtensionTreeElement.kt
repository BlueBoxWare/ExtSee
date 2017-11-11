package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ColoredItemPresentation
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.NavigatablePsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPsiUtil
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
abstract class ExtSeeExtensionTreeElement(
        val navigationElement: NavigatablePsiElement,
        val callableDescriptor: CallableDescriptor,
        val isInHerited: Boolean
): StructureViewTreeElement {

  abstract fun getIcon(): Icon

  override fun navigate(requestFocus: Boolean) = navigationElement.navigate(requestFocus)

  override fun getPresentation(): ColoredItemPresentation = object : ColoredItemPresentation {
    override fun getLocationString(): String? = navigationElement.getLocationString()

    override fun getIcon(unused: Boolean): Icon? = this@ExtSeeExtensionTreeElement.getIcon()

    override fun getPresentableText(): String? = navigationElement.presentation?.presentableText

    override fun getTextAttributesKey(): TextAttributesKey? =
            if (isInHerited) {
              CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
            } else if (navigationElement is KtModifierListOwner && KtPsiUtil.isDeprecated(navigationElement)) {
              CodeInsightColors.DEPRECATED_ATTRIBUTES
            } else {
              null
            }
  }

  override fun getChildren(): Array<TreeElement> = arrayOf()

  override fun canNavigate(): Boolean = navigationElement.canNavigate()

  override fun getValue(): Any = navigationElement

  override fun canNavigateToSource(): Boolean = navigationElement.canNavigateToSource()

}