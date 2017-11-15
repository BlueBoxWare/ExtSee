package com.gmail.blueboxware.extsee

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.FileStructureTestFixture
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase


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
class ExtSeeFileStructureTestBase: CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {

  fun testKotlin() = doTest("TestKotlin.kt", "TestKotlin.tree")

  private var popupFixture: FileStructureTestFixture? = null

  private fun doTest(fileName: String, expectedResultsFileName: String) {
    myFixture.configureByFile(absolutePath(fileName))
    popupFixture?.let { popupFixture ->
      popupFixture.update()
      UsefulTestCase.assertSameLinesWithFile(
              absolutePath(expectedResultsFileName),
              PlatformTestUtil.print(popupFixture.tree, true).trim()
      )
    } ?: throw AssertionError()
  }

  private fun absolutePath(relativePath: String): String =
          System.getProperty("user.dir") + "/src/test/testData/" + relativePath

  override fun setUp() {
    super.setUp()
    popupFixture = FileStructureTestFixture(myFixture)
  }

  override fun tearDown() {
    popupFixture?.let {
      Disposer.dispose(it)
    }
    popupFixture = null
    super.tearDown()
  }

}