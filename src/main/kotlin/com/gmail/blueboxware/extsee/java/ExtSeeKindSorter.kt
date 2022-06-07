package com.gmail.blueboxware.extsee.java

import com.gmail.blueboxware.extsee.ExtSeeExtensionTreeElement
import com.intellij.ide.structureView.impl.java.*
import java.util.*


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
internal class ExtSeeKindSorter(private val isPopup: Boolean) : KindSorter(isPopup) {

    override fun getComparator(): Comparator<*> = Comparator<Any> { o1, o2 ->
        getWeight(o1, isPopup) - getWeight(o2, isPopup)
    }

    companion object {
        val INSTANCE = ExtSeeKindSorter(false)
        val POPUP_INSTANCE = ExtSeeKindSorter(true)

        private fun getWeight(value: Any, isPopup: Boolean): Int = when (value) {
            is JavaAnonymousClassTreeElement -> 55
            is JavaClassTreeElement -> if (isPopup) 53 else 10
            is ClassInitializerTreeElement -> 15
            is SuperTypeGroup -> 20
            is PsiMethodTreeElement -> if (value.method.isConstructor) 30 else 35
            is ExtSeeExtensionTreeElement -> 35
            is PropertyGroup -> 40
            is PsiFieldTreeElement -> 50
            else -> 60
        }

    }

}
