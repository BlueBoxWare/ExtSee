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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.impl.light.LightMethodBuilder
import org.jetbrains.kotlin.psi.KtCallableDeclaration

class ExtensionAsPsiMethod(
    private val callableDeclaration: KtCallableDeclaration,
    private val _returnType: PsiType,
    accessibility: String,
    private val originClass: PsiClass
) : LightMethodBuilder(
    originClass.manager, callableDeclaration.nameAsSafeName.asString()
) {

    init {
        addModifier(accessibility)
    }

    override fun getContainingClass(): PsiClass = originClass

    override fun getReturnType(): PsiType = _returnType

    override fun hashCode(): Int = callableDeclaration.hashCode()

    override fun equals(o: Any?): Boolean = callableDeclaration == (o as? ExtensionAsPsiMethod)?.callableDeclaration

}
