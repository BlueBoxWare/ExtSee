package com.gmail.blueboxware.extsee.java

import com.gmail.blueboxware.extsee.ExtSeeExtensionTreeElement
import com.intellij.ui.LayeredIcon
import com.intellij.util.IconUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import javax.swing.Icon
import javax.swing.SwingConstants


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
internal class ExtSeeJavaExtensionTreeElement(
        callableDeclaration: KtCallableDeclaration,
        callableDescriptor: CallableDescriptor,
        isInHerited: Boolean
): ExtSeeExtensionTreeElement(callableDeclaration, callableDescriptor, isInHerited) {

  override fun getBaseIcon(): Icon = ICON

  companion object {
    val ICON = LayeredIcon(2).apply {
      setIcon(KotlinIcons.LAMBDA, 0)
      setIcon(IconUtil.scale(KotlinIcons.SMALL_LOGO, .5), 1, SwingConstants.SOUTH_EAST)
    }
  }

}