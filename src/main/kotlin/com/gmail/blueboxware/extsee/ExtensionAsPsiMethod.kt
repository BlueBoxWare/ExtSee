package com.gmail.blueboxware.extsee

import com.intellij.psi.PsiClass
import com.intellij.psi.impl.light.LightMethodBuilder
import org.jetbrains.kotlin.psi.KtCallableDeclaration

class ExtensionAsPsiMethod(
    val callableDeclaration: KtCallableDeclaration, accessibility: String, val originClass: PsiClass
) : LightMethodBuilder(
    originClass.manager, callableDeclaration.nameAsSafeName.asString()
) {

    init {
        addModifier(accessibility)
    }

    override fun getContainingClass(): PsiClass? = originClass

    override fun hashCode(): Int = callableDeclaration.hashCode()

    override fun equals(o: Any?): Boolean = callableDeclaration == (o as? ExtensionAsPsiMethod)?.callableDeclaration

}
