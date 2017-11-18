package com.gmail.blueboxware.extsee

import com.gmail.blueboxware.extsee.java.ExtSeeJavaExtensionsNodeProvider
import com.gmail.blueboxware.extsee.java.ExtSeeJavaInheritedExtensionsNodeProvider
import com.gmail.blueboxware.extsee.kotlin.ExtSeeKotlinExtensionsNodeProvider
import com.gmail.blueboxware.extsee.kotlin.ExtSeeKotlinInheritedExtensionsNodeProvider
import com.intellij.ide.util.treeView.smartTree.TreeAction
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.FileStructureTestFixture
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


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
@RunWith(Parameterized::class)
class ExtSeeFileStructureTest: LightPlatformCodeInsightFixtureTestCase() {

  //
  // https://youtrack.jetbrains.com/issue/IDEA-85891:
  // File Structure popup: "Show inherited members" hides some file classes and show members only for one class
  //

  companion object {

    private val javaTestCases = listOf(
            "class JavaClass1 extends JavaSubSubClass {}",
            "class JavaClass2 extends KotlinSubSubClass {}",
            "class JavaClass3 extends JavaSubFromKotlin {}",
            "class JavaClass4 extends KotlinSubFromJava {}",
            "class JavaClass5 extends JavaBaseClass implements JavaBaseInterface {}",
            "class JavaClass6<T> extends JavaContainer<T> {}"
    )

    private val kotlinTestCases = listOf(
            "class KotlinClass1: KotlinSubSubClass",
            "class KotlinClass2: JavaSubSubClass",
            "class KotlinClass3: KotlinSubFromJava",
            "class TestKotlin4: JavaSubFromKotlin",
            "class KotlinClass5: KotlinBaseClass, KotlinBaseInterface",
            "class KotlinClass6<T>: KotlinContainer<T>"
    )

    @JvmStatic
    @org.junit.runners.Parameterized.Parameters(name = "{1}")
    fun data() =
            javaTestCases.mapIndexed { index, content -> arrayOf(content, "JavaTest$index.java") } +
                    kotlinTestCases.mapIndexed { index, content -> arrayOf(content, "KotlinTest$index.kt") }

  }

  @org.junit.runners.Parameterized.Parameter(0)
  lateinit var content: String
  @org.junit.runners.Parameterized.Parameter(1)
  lateinit var filename: String

  private var popupFixture: FileStructureTestFixture? = null

  private val javaExt = ExtSeeJavaExtensionsNodeProvider::class.java
  private val kotlinExt = ExtSeeKotlinExtensionsNodeProvider::class.java
  private val javaExtInherited = ExtSeeJavaInheritedExtensionsNodeProvider::class.java
  private val kotlinExtInherited = ExtSeeKotlinInheritedExtensionsNodeProvider::class.java

  @Test
  fun test() {
    myFixture.configureByText(filename, content)
    doSubTest(
            listOf(javaExt, kotlinExt),
            filename + ".tree"
    )
    doSubTest(
            listOf(javaExtInherited, kotlinExtInherited),
            filename + ".inherited.tree"
    )
  }

  private fun doSubTest(actions: List<Class<out TreeAction>>, expectedResultsFileName: String) {
    popupFixture?.let { popupFixture ->
      invokeTestRunnable {
           listOf<Class<out TreeAction>>(javaExt, javaExtInherited, kotlinExt, kotlinExtInherited).forEach {
            popupFixture.popup.setTreeActionState(it, false)
          }
          actions.forEach {
            popupFixture.popup.setTreeActionState(it, true)
          }
          popupFixture.update()
          UsefulTestCase.assertSameLinesWithFile(
                  testDataPath + expectedResultsFileName,
                  PlatformTestUtil.print(popupFixture.tree, true).trim()
          )
        }
    } ?: throw AssertionError()
  }

  override fun getTestDataPath(): String = System.getProperty("user.dir") + "/src/test/testData/"

  @Before
  fun before() {
    setUp()
    invokeTestRunnable {
      myFixture.copyDirectoryToProject("project", "")

      popupFixture = FileStructureTestFixture(myFixture)
    }
  }

  @After
  fun after() {
    invokeTestRunnable {
      popupFixture?.let {
        Disposer.dispose(it)
      }
      popupFixture = null
    }
    super.tearDown()
  }

}