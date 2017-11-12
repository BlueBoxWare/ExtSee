package com.gmail.blueboxware.extsee.kotlin

import com.gmail.blueboxware.extsee.ExtSeeExtensionTreeElement
import com.intellij.psi.NavigatablePsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon


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
class ExtSeeKotlinExtensionTreeElement(
        navigationElement: NavigatablePsiElement,
        callableDescriptor: CallableDescriptor,
        isInHerited: Boolean
): ExtSeeExtensionTreeElement(navigationElement, callableDescriptor, isInHerited) {

  override fun getBaseIcon(): Icon = ICON

  companion object {
    val ICON: Icon = KotlinIcons.LAMBDA
  }

}