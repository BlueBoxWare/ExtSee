package com.gmail.blueboxware.extsee

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.java.AccessLevelProvider
import com.intellij.ide.structureView.impl.java.PsiMethodTreeElement
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.navigation.ColoredItemPresentation
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.ui.icons.IconWrapperWithToolTip
import com.intellij.ui.icons.RowIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.base.psi.getReturnTypeReference
import org.jetbrains.kotlin.idea.structureView.AbstractKotlinStructureViewElement
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.isPublic
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

class ExtSeePresentation(
    val location: String?, val textAttributes: TextAttributesKey?, val text: String, val icon: Icon?
) : ColoredItemPresentation {
    override fun getTextAttributesKey() = textAttributes

    override fun getPresentableText(): String? = text

    override fun getIcon(unused: Boolean): Icon? = icon

    override fun getLocationString(): String? = location
}

class ExtSeeExtensionTreeElement(
    val callableDeclaration: KtCallableDeclaration,
    originClass: PsiClass,
    val isInHerited: Boolean,
    val isParameterType: Boolean,
    accessibility: String
) : PsiMethodTreeElement(ExtensionAsPsiMethod(callableDeclaration, accessibility, originClass), isInHerited),
    AccessLevelProvider, AbstractKotlinStructureViewElement {

    private val myLocationString = callableDeclaration.getLocationString()

    private val canNavigateToSource = callableDeclaration.canNavigateToSource()

    private val myIsPublic = callableDeclaration.isPublic || PsiManager.getInstance(callableDeclaration.project)
        .isInProject(callableDeclaration)

    private val myPresentableText = createPresentableText(callableDeclaration, isParameterType)
    private val myTextAttributesKey = createTextAttributesKey(callableDeclaration, isInHerited)
    private val myIcon = getIcon(false)?.apply { prepareIcon(callableDeclaration) }
    private val myPresentation = ExtSeePresentation(
        myLocationString, myTextAttributesKey, myPresentableText, myIcon
    )

    override fun navigate(requestFocus: Boolean) {
        ExtSeeCoroutineService.navigate(callableDeclaration, requestFocus)
    }

    override fun canNavigate(): Boolean = true

    override fun getValue(): PsiMethod? = method

    override fun getAlphaSortKey(): String = myPresentableText

    override fun canNavigateToSource(): Boolean = canNavigateToSource

    override fun getPresentation(): ColoredItemPresentation = myPresentation

    override fun getPresentableText(): String = myPresentableText

    override fun getKey(): Any = myPresentableText

    override fun getChildrenBase(): MutableCollection<StructureViewTreeElement> = mutableListOf()

    override fun getAccessLevel(): Int = callableDeclaration.visibility()

    override fun getSubLevel(): Int = 0

    override val isPublic: Boolean
        @JvmName("foobar") get() = myIsPublic

    override val accessLevel: Int? = callableDeclaration.visibility()

    override val element: NavigatablePsiElement = callableDeclaration

    companion object {

        private fun createPresentableText(
            callableDeclaration: KtCallableDeclaration, isParameterType: Boolean
        ): String {

            val receiver = callableDeclaration.receiverTypeReference?.typeElement?.getShortTypeText()?.let {
                if (isParameterType) "on <$it>"
                else "on $it"
            } ?: ""
            val name = callableDeclaration.name ?: "<ERROR>"
            val params =
                if (callableDeclaration is KtProperty) "" else "(" + callableDeclaration.valueParameters.joinToString(", ") {
                    it.typeReference?.typeElement?.getShortTypeText() ?: "<ERROR>"
                } + ")"
            val returnType =
                callableDeclaration.getReturnTypeReference()?.typeElement?.getShortTypeText()?.let { ": $it" } ?: ""
            return "$name$params$returnType $receiver"

        }

        private fun createTextAttributesKey(callableDeclaration: KtCallableDeclaration, isInHerited: Boolean) = when {
            isInHerited -> CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
            KtPsiUtil.isDeprecated(callableDeclaration) -> CodeInsightColors.DEPRECATED_ATTRIBUTES
            else -> null
        }

        private fun Icon?.prepareIcon(callableDeclaration: KtCallableDeclaration) {
            val isProperty = callableDeclaration is KtProperty
            val isWriteable = (callableDeclaration as? KtProperty)?.isVar == true
            val icon = if (isProperty) PROPERTY_ICON else FUNCTION_ICON
            val tooltip =
                if (isProperty) (if (isWriteable) "Writeable Extension Property" else "Read-only Extension Property") else "Extension Function"
            val withTooltip = IconWrapperWithToolTip(icon) { tooltip }
            (this as? RowIcon)?.setIcon(withTooltip, 0)
        }

        private val FUNCTION_ICON: Icon = KotlinIcons.LAMBDA
        private val PROPERTY_ICON: Icon = AllIcons.Nodes.Variable

    }

}

@Service(Service.Level.PROJECT)
internal class ExtSeeCoroutineService(
    val coroutineScope: CoroutineScope
) {

    companion object {

        fun navigate(callableDeclaration: KtCallableDeclaration, requestFocus: Boolean) {
            callableDeclaration.project.service<ExtSeeCoroutineService>().coroutineScope.launch(Dispatchers.EDT) {
                val descriptor = withContext(Dispatchers.Default) {
                    readAction {
                        PsiNavigationSupport.getInstance().getDescriptor(callableDeclaration)
                    }
                }
                descriptor?.navigate(requestFocus)
            }
        }
    }

}
