package com.gmail.blueboxware.extsee

import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.KeyedFactoryEPBean
import com.intellij.openapi.progress.ProgressIndicator


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
class ExtSeeStartupActivity: PreloadingActivity() {

  override fun preload(indicator: ProgressIndicator) {
    Extensions.getRootArea().getExtensionPoint<KeyedFactoryEPBean>("com.intellij.structureViewBuilder").let { extensionPoint ->
      extensionPoint.extensions.find { it.factoryClass == "org.jetbrains.kotlin.idea.structureView.KtClsStructureViewBuilderProvider" }?.let { extension ->
        extensionPoint.unregisterExtension(extension)
      }
    }
  }

}