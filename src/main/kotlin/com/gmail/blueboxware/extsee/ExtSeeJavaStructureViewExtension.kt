package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.StructureViewExtension
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

internal class ExtSeeJavaStructureViewExtension : StructureViewExtension {
    override fun getType(): Class<out PsiElement?>? = PsiClass::class.java

    override fun getChildren(parent: PsiElement): Array<out StructureViewTreeElement?>? {
        return findExtensions(parent).toTypedArray()
    }

    override fun getCurrentEditorElement(
        editor: Editor?,
        parent: PsiElement?
    ): Any? = null
}