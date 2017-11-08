package com.gmail.blueboxware.extsee.kotlin

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile


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
class ExtSeeKotlinStructureViewFactory : PsiStructureViewFactory {

  override fun getStructureViewBuilder(psiFile: PsiFile?): StructureViewBuilder? =
          (psiFile as? KtFile)?.let { ktFile ->
            object: TreeBasedStructureViewBuilder() {
              override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                      ExtSeeKotlinStructureViewModel(ktFile)
            }
          }

}