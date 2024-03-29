package com.gmail.blueboxware.extsee.java

import com.intellij.ide.structureView.StructureView
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile


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
internal class ExtSeeJavaStructureViewFactory : PsiStructureViewFactory {

    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? =

        (psiFile as? PsiJavaFile)?.let { psiJavaFile ->
            object : TreeBasedStructureViewBuilder() {

                override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                    ExtSeeJavaStructureViewModel(psiJavaFile, editor)

                override fun isRootNodeShown(): Boolean = false

                override fun createStructureView(fileEditor: FileEditor?, project: Project): StructureView {
                    val structureView = super.createStructureView(fileEditor, project)
                    (structureView.treeModel as? ExtSeeJavaStructureViewModel)?.structureView = structureView
                    return structureView
                }
            }
        }

}
