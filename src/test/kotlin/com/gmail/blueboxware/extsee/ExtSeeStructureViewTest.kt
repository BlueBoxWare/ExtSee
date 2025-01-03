package com.gmail.blueboxware.extsee

import com.intellij.ide.structureView.impl.java.*
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.runInEdtAndWait
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
class ExtSeeStructureViewTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {

    companion object {

        private val javaSimpleTestCases = listOf(
            " extends JavaSubSubClass",
            " extends KotlinSubSubClass",
            " extends JavaSubFromKotlin",
            " extends KotlinSubFromJava",
            " extends JavaBaseClass implements JavaBaseInterface",
            " extends JavaContainer<T>",
            " extends com.example.LibJavaClass"
        )

        private val kotlinSimpleTestCases = listOf(
            ": KotlinSubSubClass",
            ": JavaSubSubClass",
            ": KotlinSubFromJava",
            ": JavaSubFromKotlin",
            ": KotlinBaseClass, KotlinBaseInterface",
            "<T>: KotlinContainer<T>"
        )

        private val fileTests = listOf(
            "MyObject.java", "Collections.kt", "Generics.kt", "Misc.kt", "Properties.kt", "Generics.java"
        )

        val DEFAULT_ACTIONS = listOf(
            Sorter.getAlphaSorterId()
        )

        private val actionSetsToTest: Map<String, Collection<String>> = mapOf(
            "Default" to emptyList(),
            "Inherited" to listOf(JavaInheritedMembersNodeProvider().name),
            "Inherited, public only" to listOf(PublicElementsFilter.ID, JavaInheritedMembersNodeProvider().name),
            "Inherited, grouped" to listOf(JavaInheritedMembersNodeProvider().name, SuperTypesGrouper.ID)
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}: {3}")
        fun data() = (javaSimpleTestCases.mapIndexed { index, content ->
            listOf(
                "JavaSimpleTest$index.java", "class JavaClass$content {}"
            )
        } + kotlinSimpleTestCases.mapIndexed { index, content ->
            listOf(
                "KotlinSimpleTest$index.kt", "class KotlinClass$content"
            )
        } + fileTests.map { filename: String ->
            listOf(
                "files/$filename", null
            )
        }).flatMap { inputs -> actionSetsToTest.map { (inputs + listOf(it.value) + it.key).toTypedArray() } }


        val TEST_DATA_PATH = System.getProperty("user.dir") + "/src/test/testData/"

        private var jdk: Sdk? = null
    }

    @Parameterized.Parameter(0)
    @JvmField
    var filename: String = ""

    @Parameterized.Parameter(1)
    @JvmField
    var content: String? = null

    @Parameterized.Parameter(2)
    @JvmField
    var actions: Collection<String> = emptyList()

    @Parameterized.Parameter(3)
    @JvmField
    var actionSetName: String? = null

    @Test
    fun test() {

        if (content != null) {
            myFixture.configureByText(filename, content!!)
        } else {
            runInEdtAndWait {
                myFixture.configureByFile(filename)
            }
        }

        val expectedFileSuffix = "." + actionSetName!!.replace(", ", "_").replace(' ', '_').lowercase()

        runInEdtAndWait {
            myFixture.testStructureView { structureView ->

                (DEFAULT_ACTIONS + actions).forEach {
                    structureView.setActionActive(it, true)
                }

                PlatformTestUtil.expandAll(structureView.tree)

                assertSameLinesWithFile(
                    TEST_DATA_PATH + "results/" + filename + expectedFileSuffix,
                    PlatformTestUtil.print(structureView.tree, false),
                    true
                ) {
                    content ?: ""
                }

            }

        }
    }


    private fun before() {

        if (jdk == null) {
            jdk = IdeaTestUtil.createMockJdk("java 0.8", TEST_DATA_PATH + "libs/mockJDK-1.8")
        }

        runInEdtAndWait {
            runWriteAction {
                ProjectJdkTable.getInstance().addJdk(jdk!!)
                ModuleRootManager.getInstance(myFixture.module).modifiableModel.let {
                    it.sdk = jdk
                    it.commit()
                }

            }
        }

        myFixture.testDataPath = TEST_DATA_PATH

        myFixture.copyDirectoryToProject("project", "")
        PsiTestUtil.addLibrary(myFixture.module, "$TEST_DATA_PATH/libs/dummyLib.jar")

    }

    override fun setUp() {
        super.setUp()
        before()
    }

    override fun tearDown() {
        runWriteAction {
            ProjectJdkTable.getInstance().removeJdk(jdk!!)
        }
        super.tearDown()
    }


}
