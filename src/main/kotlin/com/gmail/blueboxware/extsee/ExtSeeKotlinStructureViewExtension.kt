package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.StructureViewExtension
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass

internal class ExtSeeKotlinStructureViewExtension : StructureViewExtension {
    override fun getType(): Class<out PsiElement?>? = KtClass::class.java

    override fun getChildren(parent: PsiElement): Array<out StructureViewTreeElement?>? {
        return findExtensions(parent).toTypedArray()
    }

    override fun getCurrentEditorElement(
        editor: Editor?,
        parent: PsiElement?
    ): Any? = null
}