package com.gmail.blueboxware.extsee

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent


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
internal abstract class ExtSeePsiTreeChangeAdapter: PsiTreeChangeAdapter() {

  abstract fun onChanged(vararg elements: PsiElement)

  override fun childReplaced(event: PsiTreeChangeEvent) {
    if (!event.newChild.isInBody()) {
      onChanged(event.newChild)
    }
  }

  override fun childrenChanged(event: PsiTreeChangeEvent) {

  }

  override fun propertyChanged(event: PsiTreeChangeEvent) {

  }

  override fun childMoved(event: PsiTreeChangeEvent) {

  }

  override fun childRemoved(event: PsiTreeChangeEvent) {
    if (!event.parent.isInBody()) {
      onChanged()
    }
  }

  override fun childAdded(event: PsiTreeChangeEvent) {
    if (!event.child.isInBody()) {
      onChanged()
    }
  }

}