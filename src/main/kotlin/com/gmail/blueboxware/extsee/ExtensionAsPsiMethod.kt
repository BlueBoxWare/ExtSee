package com.gmail.blueboxware.extsee

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.impl.light.LightMethodBuilder
import org.jetbrains.kotlin.psi.KtCallableDeclaration

class ExtensionAsPsiMethod(
    private val callableDeclaration: KtCallableDeclaration,
    private val _returnType: PsiType,
    accessibility: String,
    private val originClass: PsiClass
) : LightMethodBuilder(
    originClass.manager, callableDeclaration.nameAsSafeName.asString()
) {

    init {
        addModifier(accessibility)
    }

    override fun getContainingClass(): PsiClass = originClass

    override fun getReturnType(): PsiType = _returnType

    override fun hashCode(): Int = callableDeclaration.hashCode()

    override fun equals(o: Any?): Boolean = callableDeclaration == (o as? ExtensionAsPsiMethod)?.callableDeclaration

}
