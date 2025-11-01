/*
 * Copyright (c) 2025 BlueBoxWare
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.StructureViewExtension
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

internal class ExtSeeJavaStructureViewExtension : StructureViewExtension {
    override fun getType(): Class<out PsiElement?> = PsiClass::class.java

    override fun getChildren(parent: PsiElement): Array<out StructureViewTreeElement?> {
        return findExtensions(parent).toTypedArray()
    }

    override fun getCurrentEditorElement(
        editor: Editor?,
        parent: PsiElement?
    ): Any? = null
}
