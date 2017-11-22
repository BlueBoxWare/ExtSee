package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ColoredItemPresentation
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.Iconable
import com.intellij.psi.util.PsiUtil
import com.intellij.ui.RowIcon
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.renderer.DescriptorRenderer
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
        val callableDeclaration: KtCallableDeclaration,
        callableDescriptor: CallableDescriptor,
        val isInHerited: Boolean
): StructureViewTreeElement {

  val accessLevel = callableDescriptor.visibility.let { visibility ->
    when (visibility) {
      Visibilities.PUBLIC -> PsiUtil.ACCESS_LEVEL_PUBLIC
      Visibilities.INTERNAL -> PsiUtil.ACCESS_LEVEL_PACKAGE_LOCAL
      else -> -1
    }
  }

  private val myLocationString = callableDeclaration.getLocationString()
  private val myIcon = createIcon(getBaseIcon(), callableDeclaration, callableDescriptor)
  private val myPresentableText = createPresentableText(callableDeclaration, callableDescriptor)
  private val myTextAttributesKey = createTextAttributesKey(callableDeclaration, isInHerited)
  private val myPresentation = object: ColoredItemPresentation {
    override fun getLocationString(): String? = myLocationString
    override fun getIcon(unused: Boolean): Icon? = myIcon
    override fun getTextAttributesKey(): TextAttributesKey? = myTextAttributesKey
    override fun getPresentableText(): String? = myPresentableText
  }

  abstract fun getBaseIcon(): Icon

  override fun navigate(requestFocus: Boolean) = callableDeclaration.navigate(requestFocus)

  override fun getChildren(): Array<TreeElement> = arrayOf()

  override fun canNavigate(): Boolean = callableDeclaration.canNavigate()

  override fun getValue(): Any = callableDeclaration

  override fun canNavigateToSource(): Boolean = callableDeclaration.canNavigateToSource()

  override fun getPresentation(): ColoredItemPresentation = myPresentation

  companion object {

    fun createIcon(baseIcon: Icon, callableDeclaration: KtCallableDeclaration, callableDescriptor: CallableDescriptor): Icon? {

      val icon = KotlinDescriptorIconProvider.getIcon(callableDescriptor, callableDeclaration, Iconable.ICON_FLAG_VISIBILITY)
      (icon as? RowIcon)?.setIcon(baseIcon, 0)

      return icon

    }

    fun createPresentableText(callableDeclaration: KtCallableDeclaration, callableDescriptor: CallableDescriptor) =
            callableDeclaration.presentation?.presentableText +
                    (callableDescriptor.extensionReceiverParameter?.type?.let { receiverType ->
                      DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.renderType(receiverType).let { str ->
                        " on $str"
                      }
                    } ?: "")

    fun createTextAttributesKey(callableDeclaration: KtCallableDeclaration, isInHerited: Boolean) =
            if (isInHerited) {
              CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
            } else if (KtPsiUtil.isDeprecated(callableDeclaration)) {
              CodeInsightColors.DEPRECATED_ATTRIBUTES
            } else {
              null
            }

  }

}