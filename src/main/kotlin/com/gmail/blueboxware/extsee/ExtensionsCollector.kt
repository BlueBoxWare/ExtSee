package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.TextEditorBasedStructureViewModel
import com.intellij.ide.structureView.newStructureView.StructureViewComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.util.concurrent.ConcurrentHashMap


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
class ExtensionsCollector(private val project: Project, private val model: TextEditorBasedStructureViewModel): Disposable {

  init {
    PsiManager.getInstance(project).addPsiTreeChangeListener(object: ExtSeePsiTreeChangeAdapter () {
      override fun onChanged(vararg elements: PsiElement) {
        extensions.clear()
      }
    }, this)
  }

  private val extensions = ConcurrentHashMap<Pair<PsiElement, Boolean>, Collection<ExtSeeExtensionTreeElement>>()
  private var isDisposed = false

  fun getExtensions(element: PsiElement, inherited: Boolean): Collection<ExtSeeExtensionTreeElement> {

    extensions[element to inherited]?.let {
      return it
    }

    val result = BackgroundTaskUtil.computeInBackgroundAndTryWait(
            {
              DumbService.getInstance(project).runReadActionInSmartMode(Computable {
                findExtensions(element, inherited) { !isDisposed }
              })
            },
            { lateResult ->

              extensions[element to inherited] = lateResult

                ApplicationManager.getApplication().invokeLater {
                  if (!isDisposed) {
                    ((model as? ExtSeeStructureViewModel)?.structureView as? StructureViewComponent)?.setActionActive("", true)
                  }
                }

            },
            TIMEOUT
    )

    if (result != null) {
      extensions[element to inherited] = result
    }

    return result ?: listOf()
  }

 override fun dispose() {
    isDisposed = true
    extensions.clear()
  }

  companion object {

    val TIMEOUT = if (ApplicationManager.getApplication().isUnitTestMode) 99999L else 500L

  }

}