package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.StructureViewFactoryEx
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.impl.LibraryScopeCache
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.base.analysis.api.utils.KtSymbolFromIndexProvider
import org.jetbrains.kotlin.idea.base.analysis.api.utils.isPossiblySubTypeOf
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments
import java.awt.KeyboardFocusManager

/*
 * Copyright 2017-2025 Blue Box Ware
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

private val acceptableVisibilities = listOf(
    KaSymbolVisibility.PACKAGE_PROTECTED, KaSymbolVisibility.INTERNAL, KaSymbolVisibility.PUBLIC
)

private const val INHERITED_ACTION_ID = "SHOW_INHERITED"
private const val STRUCTURE_POPUP_INHERITED_ID = "SHOW_INHERITED.file.structure.state"

private class Result(
    val declaration: KtCallableDeclaration, val origin: PsiClass, val isParameter: Boolean, val isInherited: Boolean
)

@OptIn(KaExperimentalApi::class)
internal fun findExtensions(
    element: PsiElement
): Collection<ExtSeeExtensionTreeElement> {

    if (DumbService.isDumb(element.project)) return emptyList()

    var ktFile: KtFile? = null
    FileTypeIndex.processFiles(KotlinFileType.INSTANCE, { file ->
        if (file.extension != KotlinFileType.EXTENSION) {
            true
        } else {
            ktFile = file.toPsiFile(element.project) as? KtFile
            false
        }
    }, element.project.projectScope())
    if (ktFile == null) {
        FileTypeIndex.processFiles(KotlinFileType.INSTANCE, { file ->
            ktFile = file.toPsiFile(element.project) as? KtFile
            false
        }, LibraryScopeCache.getInstance(element.project).librariesOnlyScope)
    }
    val ktElement = (element as? KtElement) ?: ktFile ?: return emptyList()
    val searchScopeFile = ktFile ?: return emptyList()

    val typeFqName: String = analyze(ktElement) {
        when (element) {
            is KtClassOrObject -> element.namedClassSymbol?.classId?.asString()
            is PsiClass -> element.namedClassSymbol?.classId?.asString()
            else -> null
        } ?: return emptyList()
    }

    var includeInherited = typeFqName == "java.lang.Object"
    if (!includeInherited) {
        includeInherited =
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner?.let { it::class.qualifiedName } == "com.intellij.ide.util.FileStructurePopup.MyTree") {
                PropertiesComponent.getInstance().getBoolean(STRUCTURE_POPUP_INHERITED_ID)
            } else {
                StructureViewFactoryEx.getInstanceEx(element.project).isActionActive(INHERITED_ACTION_ID)
            }
    }
    if (!includeInherited && isUnitTestMode()) {
        ((StructureViewFactoryEx.getInstanceEx(element.project) as? PersistentStateComponent<*>)?.state)?.let { state ->
            includeInherited =
                (state::class.java.getField("ACTIVE_ACTIONS").get(state) as? String)?.contains("SHOW_INHERITED") == true
        }
    }

    return analyze(searchScopeFile) {

        buildClassType(ClassId.fromString(typeFqName)).let { type ->

            KtSymbolFromIndexProvider(searchScopeFile).getExtensionCallableSymbolsByNameFilter(
                { true }, listOfNotNull(type)
            ).mapNotNull { extension ->
                if (extension.visibility !in acceptableVisibilities) {
                    return@mapNotNull null
                }
                val receiverType = try {
                    extension.receiverType
                } catch (_: KotlinIllegalArgumentExceptionWithAttachments) {
                    null
                } ?: return@mapNotNull null

                val isApplicable = isApplicable(type, receiverType)
                if (!isApplicable.isApplicable) {
                    return@mapNotNull null
                }

                val origin = (isApplicable.origin?.asPsiType(
                    element, allowErrorTypes = true
                ) as? PsiClassReferenceType)?.resolve() ?: element.manager?.let {
                    PsiType.getJavaLangObject(
                        it, GlobalSearchScope.allScope(element.project)
                    ).resolve()
                } ?: return@mapNotNull null
                (extension.psi?.navigationElement?.navigationElement as? KtCallableDeclaration)?.let {
                    Result(
                        it,
                        origin,
                        isInherited = !isApplicable.isDirect,
                        isParameter = receiverType is KaTypeParameterType
                    )
                }
            }.filter {
                includeInherited || !it.isInherited
            }.map {
                ExtSeeExtensionTreeElement(
                    it.declaration,
                    originClass = it.origin,
                    isInHerited = it.isInherited,
                    isParameterType = it.isParameter,
                    it.declaration.visibilityString()
                )
            }.toSet()
        }

    }

}

private class ApplicableResult private constructor(
    val isApplicable: Boolean, val isDirect: Boolean, val origin: KaClassType?
) {
    companion object {
        val NO = ApplicableResult(isApplicable = false, isDirect = false, null)
        fun direct(origin: KaClassType?) = ApplicableResult(isApplicable = true, isDirect = true, origin)
        fun indirect(origin: KaClassType?) = ApplicableResult(isApplicable = true, isDirect = false, origin)
    }
}

@OptIn(KaExperimentalApi::class)
private fun KaSession.isApplicable(type: KaType, extensionType: KaType): ApplicableResult {
    if (type is KaClassType && extensionType is KaClassType) {
        if (type.classId == extensionType.classId) {
            if (type.typeArguments.size == extensionType.typeArguments.size) {
                type.typeArguments.zip(extensionType.typeArguments).map { (type, extType) ->
                    if (!isApplicable(type, extType)) {
                        return ApplicableResult.NO
                    }
                }
                return ApplicableResult.direct(type)
            } else if (type.symbol.typeParameters.size == extensionType.typeArguments.size) {
                return ApplicableResult.direct(type)
            }
        }

    }

    if (extensionType is KaTypeParameterType) {
        for (upperBound in extensionType.directSupertypes) {
            val result = isApplicable(type, upperBound)
            if (result.isApplicable) {
                return result
            }
        }
    }

    var supertypes = type.directSupertypes.toList()
    if (supertypes.isEmpty()) {
        supertypes = (type.symbol as? KaClassSymbol)?.superTypes ?: emptyList()
    }

    for (superType in supertypes) {
        val result = isApplicable(superType, extensionType)
        if (result.isApplicable) {
            return ApplicableResult.indirect(result.origin)
        }
    }

    return ApplicableResult.NO

}

private fun KaSession.isApplicable(typeArgument: KaTypeProjection, extensionTypeArgument: KaTypeProjection): Boolean {
    if (extensionTypeArgument is KaStarTypeProjection) {
        return true
    }

    val type = typeArgument.type ?: return false
    val extType = extensionTypeArgument.type ?: return false

    val variance = (extensionTypeArgument as? KaTypeArgumentWithVariance)?.variance ?: Variance.INVARIANT

    if (variance == Variance.INVARIANT && extType is KaClassType && type is KaClassType) {
        return extType.isPossiblySubTypeOf(type) && type.isPossiblySubTypeOf(extType)
    }

    var types: Collection<KaType> = listOfNotNull(type)
    while (types.any { it is KaTypeParameterType }) {
        types = types.flatMap { t -> (t as? KaTypeParameterType)?.directSupertypes ?: sequenceOf(t) }
    }

    var extTypes = listOfNotNull(extType)
    while (extTypes.any { it is KaTypeParameterType }) {
        extTypes = extTypes.flatMap { t -> (t as? KaTypeParameterType)?.directSupertypes ?: sequenceOf(t) }
    }

    val reverse = (type is KaTypeParameterType) or (variance == Variance.IN_VARIANCE)

    for (type in types) {
        for (extType in extTypes) {
            if (reverse && extType.isPossiblySubTypeOf(type)) {
                return true
            } else if (type.isPossiblySubTypeOf(extType)) {
                return true
            }
        }
    }

    return false

}

// https://github.com/JetBrains/kotlin/blob/ca0049ad0a5e87316db94e42f155a280c0520e3b/compiler/psi/src/org/jetbrains/kotlin/psi/KtTypeReference.kt
internal fun KtTypeElement.getShortTypeText(): String? {
    return when (this) {
        is KtUserType -> buildString {
            append(referencedName)
            val args = typeArguments
            if (args.isNotEmpty()) {
                append(args.joinToString(", ", "<", ">") {
                    val projection = when (it.projectionKind) {
                        KtProjectionKind.IN -> "in "
                        KtProjectionKind.OUT -> "out "
                        KtProjectionKind.STAR -> "*"
                        KtProjectionKind.NONE -> ""
                    }
                    projection + (it.typeReference?.typeElement?.getShortTypeText() ?: "")
                })
            }
        }

        is KtFunctionType -> buildString {
            val contextReceivers = contextReceiversTypeReferences
            if (contextReceivers.isNotEmpty()) {
                append(contextReceivers.joinToString(", ", "context(", ")") {
                    it.typeElement?.getShortTypeText() ?: ""
                })
            }
            receiverTypeReference?.let { append(it.typeElement?.getShortTypeText()) }
            append(parameters.joinToString(", ", "(", ")") { param ->
                param.name?.let { "$it: " }.orEmpty() + param.typeReference?.typeElement?.getShortTypeText().orEmpty()
            })
            returnTypeReference?.let { returnType ->
                append(" -> ")
                append(returnType.typeElement?.getShortTypeText())
            }
        }

        is KtIntersectionType -> getLeftTypeRef()?.typeElement?.getShortTypeText() + " & " + getRightTypeRef()?.typeElement?.getShortTypeText()

        is KtNullableType -> {
            val innerType = innerType
            buildString {
                val parenthesisRequired = innerType is KtFunctionType
                if (parenthesisRequired) {
                    append("(")
                }
                append(innerType?.getShortTypeText())
                append("?")
                if (parenthesisRequired) {
                    append(")")
                }
            }
        }

        is KtDynamicType -> "dynamic"
        else -> "<ERROR>"
    }
}

internal fun NavigatablePsiElement.getLocationString(): String? {

    val virtualFile = containingFile?.virtualFile ?: return null

    var location = virtualFile.nameWithoutExtension
    (containingFile as? KtFile)?.packageFqName?.asString()?.let { packageFqName ->
        if (packageFqName != "") {
            location = "$packageFqName.$location"
        }
    }

    return "in $location"

}

internal fun KtModifierListOwner.visibility(): Int = when {
    modifierList?.hasModifier(PRIVATE_KEYWORD) == true -> PsiUtil.ACCESS_LEVEL_PRIVATE
    modifierList?.hasModifier(PROTECTED_KEYWORD) == true -> PsiUtil.ACCESS_LEVEL_PROTECTED
    modifierList?.hasModifier(INTERNAL_KEYWORD) == true -> PsiUtil.ACCESS_LEVEL_PACKAGE_LOCAL
    else -> PsiUtil.ACCESS_LEVEL_PUBLIC
}

internal fun KtModifierListOwner.visibilityString(): String = when {
    modifierList?.hasModifier(PRIVATE_KEYWORD) == true -> PsiModifier.PRIVATE
    modifierList?.hasModifier(PROTECTED_KEYWORD) == true -> PsiModifier.PROTECTED
    modifierList?.hasModifier(INTERNAL_KEYWORD) == true -> PsiModifier.PACKAGE_LOCAL
    else -> PsiModifier.PUBLIC
}

