/*
 * Copyright (c) 2017-2025 Blue Box Ware
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
package com.gmail.blueboxware.extsee

import com.gmail.blueboxware.extsee.settings.ExtSeeSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.modules
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.contextModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.utils.classId
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.base.analysis.api.utils.KtSymbolFromIndexProvider
import org.jetbrains.kotlin.idea.base.analysis.api.utils.isPossiblySubTypeOf
import org.jetbrains.kotlin.idea.base.projectStructure.getKaModule
import org.jetbrains.kotlin.idea.base.projectStructure.toKaSourceModuleForProductionOrTest
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.base.util.restrictToKotlinSources
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments
import java.awt.KeyboardFocusManager

private val acceptableVisibilities = listOf(
    KaSymbolVisibility.PACKAGE_PROTECTED, KaSymbolVisibility.INTERNAL, KaSymbolVisibility.PUBLIC
)

private class Result(
    val declaration: KtCallableDeclaration,
    val returnType: PsiType,
    val origin: PsiClass,
    val isParameter: Boolean,
    val isInherited: Boolean


) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Result

        if (isParameter != other.isParameter) return false
        if (isInherited != other.isInherited) return false
        if (declaration != other.declaration) return false
        if (returnType != other.returnType) return false
        if (origin != other.origin) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isParameter.hashCode()
        result = 31 * result + isInherited.hashCode()
        result = 31 * result + declaration.hashCode()
        result = 31 * result + returnType.hashCode()
        result = 31 * result + origin.hashCode()
        return result
    }
}

@OptIn(KaExperimentalApi::class)
internal fun findExtensions(
    element: PsiElement
): Collection<ExtSeeExtensionTreeElement> {

    if (DumbService.isDumb(element.project)) return emptyList()

    val settings = element.project.service<ExtSeeSettings>()
    val isPopup =
        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner?.let { it::class.qualifiedName } == "com.intellij.ide.util.FileStructurePopup.MyTree"

    if ((isPopup && !settings.showExtensionsPopup) || (!isPopup && !settings.showExtensionsToolWindow)) {
        return emptyList()
    }

    val isCompanion = (element as? KtObjectDeclaration)?.isCompanion() ?: false
    if ((isCompanion && isPopup && !settings.showOnCompanionPopup) || (isCompanion && !isPopup && !settings.showOnCompanionToolWindow)) {
        return emptyList()
    }

    var ktFile: KtFile? = element.containingFile as? KtFile
    if (ktFile == null) {
        FileTypeIndex.processFiles(KotlinFileType.INSTANCE, { file ->
            if (file.extension != KotlinFileType.EXTENSION) {
                true
            } else {
                ktFile = file.toPsiFile(element.project) as? KtFile
                false
            }
        }, element.project.projectScope().restrictToKotlinSources())
    }

    val projectKtFile = ktFile ?: return emptyList()

    val targetModule = element.getKaModule(element.project, projectKtFile.contextModule).takeIf {
        it::class.simpleName?.startsWith("KaNotUnderContentRootModule") == false
    } ?: projectKtFile.getKaModule(element.project, null)

    val classId: ClassId = analyze(targetModule) {
        if (element.canBeAnalysed()) {
            when (element) {
                is KtClassOrObject -> element.namedClassSymbol?.classId
                is PsiClass -> element.classId
                else -> null
            }
        } else {
            element.kotlinFqName?.let {
                ClassId.topLevel(it)
            }
        }
    } ?: return emptyList()

    var includeInherited = classId.asString() == "java/lang/Object"
    if (!includeInherited) {
        includeInherited = (isPopup && settings.showInheritedPopup) || (!isPopup && settings.showInheritedToolWindow)
    }

    val results = mutableSetOf<Result>()

    for (module in element.project.modules.mapNotNull { it.toKaSourceModuleForProductionOrTest() }) {

        analyze(module) {
            val type = buildClassType(classId)

            if (type is KaErrorType) {
                return@analyze
            }

            KtSymbolFromIndexProvider(projectKtFile).getExtensionCallableSymbolsByNameFilter(
                { true },
                listOfNotNull(type),
                psiFilter = { it.canBeAnalysed() }
            ).mapNotNull { extension ->
                if (extension.visibility !in acceptableVisibilities) {
                    return@mapNotNull null
                }

                val module = extension.containingModule
                if (module !is KaLibraryModule && module !is KaSourceModule && module !is KaLibrarySourceModule) {
                    return@mapNotNull null
                } else if (module::class.java.simpleName.startsWith("KaScriptDependency")) {
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

                if ((isPopup && !settings.showOnAnyPopup) || (!isPopup && !settings.showOnAnyToolWindow)) {
                    if (isApplicable.origin != null && builtinTypes.any.semanticallyEquals(isApplicable.origin)) {
                        return@mapNotNull null
                    }
                }

                val origin = (isApplicable.origin?.asPsiType(
                    element, allowErrorTypes = true
                ) as? PsiClassReferenceType)?.resolve() ?: element.manager?.let {
                    PsiType.getJavaLangObject(
                        it, GlobalSearchScope.allScope(element.project)
                    ).resolve()
                } ?: return@mapNotNull null
                (extension.psi?.navigationElement?.navigationElement as? KtCallableDeclaration)?.let {
                    extension.returnType.asPsiType(element, allowErrorTypes = true)?.let { returnType ->
                        Result(
                            it,
                            returnType,
                            origin,
                            isInherited = !isApplicable.isDirect,
                            isParameter = receiverType is KaTypeParameterType
                        )
                    }
                }
            }.filter {
                includeInherited || !it.isInherited
            }.forEach {
                results.add(it)
            }
        }
    }

    return results.map {
        ExtSeeExtensionTreeElement(
            it.declaration,
            returnType = it.returnType,
            originClass = it.origin,
            isInHerited = it.isInherited,
            isParameterType = it.isParameter,
            it.declaration.visibilityString()
        )
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


