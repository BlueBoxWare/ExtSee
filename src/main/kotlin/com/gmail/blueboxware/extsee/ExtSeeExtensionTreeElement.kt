package com.gmail.blueboxware.extsee

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.navigation.ColoredItemPresentation
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Iconable
import com.intellij.ui.LayeredIcon
import com.intellij.ui.RowIcon
import com.intellij.util.IconUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.renderer.DescriptorRenderer
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
class ExtSeeExtensionTreeElement(
    val callableDeclaration: KtCallableDeclaration,
    callableDescriptor: CallableDescriptor,
    val isInHerited: Boolean,
    fileType: FileType
) : PsiTreeElementBase<KtCallableDeclaration>(callableDeclaration) {

    val accessLevel = callableDeclaration.visibility()

    private val myLocationString = callableDeclaration.getLocationString()

    private val myIcon = createIcon(fileType, callableDeclaration, callableDescriptor)
    private val myPresentableText = createPresentableText(callableDeclaration, callableDescriptor)
    private val myTextAttributesKey = createTextAttributesKey(callableDeclaration, isInHerited)
    private val myPresentation = object : ColoredItemPresentation {
        override fun getLocationString(): String? = myLocationString
        override fun getIcon(unused: Boolean): Icon? = myIcon
        override fun getTextAttributesKey(): TextAttributesKey? = myTextAttributesKey
        override fun getPresentableText(): String = myPresentableText
    }

    override fun navigate(requestFocus: Boolean) = callableDeclaration.navigate(requestFocus)

    override fun canNavigate(): Boolean = callableDeclaration.canNavigate()

    override fun getValue() = callableDeclaration

    override fun canNavigateToSource(): Boolean = callableDeclaration.canNavigateToSource()

    override fun getPresentation(): ColoredItemPresentation = myPresentation

    override fun getPresentableText(): String = myPresentableText

    override fun getChildrenBase(): MutableCollection<StructureViewTreeElement> = mutableListOf()

    companion object {

        fun createIcon(
            fileType: FileType, callableDeclaration: KtCallableDeclaration, callableDescriptor: CallableDescriptor
        ): Icon? {

            val baseIcon = when (fileType) {
                JavaFileType.INSTANCE -> JAVA_ICON
                KotlinFileType.INSTANCE -> KOTLIN_ICON
                else -> {
                    LOGGER.error("Invalid file type: " + fileType.name)
                    null
                }
            }

            val icon = KotlinDescriptorIconProvider.getIcon(
                callableDescriptor, callableDeclaration, Iconable.ICON_FLAG_VISIBILITY
            )
            (icon as? RowIcon)?.setIcon(baseIcon, 0)

            return icon

        }

        fun createPresentableText(callableDeclaration: KtCallableDeclaration, callableDescriptor: CallableDescriptor) =
            callableDeclaration.presentation?.presentableText + (callableDescriptor.extensionReceiverParameter?.type?.let { receiverType ->
                DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.renderType(receiverType).let { str ->
                    " on $str"
                }
            } ?: "")

        fun createTextAttributesKey(callableDeclaration: KtCallableDeclaration, isInHerited: Boolean) = when {
            isInHerited -> CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
            KtPsiUtil.isDeprecated(callableDeclaration) -> CodeInsightColors.DEPRECATED_ATTRIBUTES
            else -> null
        }

        private val JAVA_ICON = LayeredIcon(2).apply {
            setIcon(KotlinIcons.LAMBDA, 0)
            setIcon(IconUtil.scale(KotlinIcons.SMALL_LOGO, null, .5f), 1, SwingConstants.SOUTH_EAST)
        }

        private val KOTLIN_ICON: Icon = KotlinIcons.LAMBDA

    }

}
