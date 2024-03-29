package com.gmail.blueboxware.extsee

import com.gmail.blueboxware.extsee.java.ExtSeeJavaExtensionsNodeProvider
import com.gmail.blueboxware.extsee.java.ExtSeeJavaInheritedExtensionsNodeProvider
import com.gmail.blueboxware.extsee.kotlin.ExtSeeKotlinExtensionsNodeProvider
import com.gmail.blueboxware.extsee.kotlin.ExtSeeKotlinInheritedExtensionsNodeProvider
import com.intellij.ide.structureView.impl.java.*
import com.intellij.ide.util.InheritedMembersNodeProvider
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.testFramework.*
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
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
            "MyObject.java", "Turtles.java", "Collections.kt", "Generics.kt"
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = javaSimpleTestCases.mapIndexed { index, content ->
            arrayOf(
                "JavaSimpleTest$index.java", "class JavaClass$content {}"
            )
        } + kotlinSimpleTestCases.mapIndexed { index, content ->
            arrayOf(
                "KotlinSimpleTest$index.kt", "class KotlinClass$content"
            )
        } + fileTests.map { filename: String ->
            arrayOf(
                "files/$filename", null
            )
        }

        val DEFAULT_ACTIONS = listOf(
            JavaInheritedMembersNodeProvider.ID,
            JavaAnonymousClassesNodeProvider.ID,
            JavaLambdaNodeProvider.ID,
            ExtSeeJavaExtensionsNodeProvider.ID,
            ExtSeeKotlinExtensionsNodeProvider.ID,
            Sorter.ALPHA_SORTER_ID,
            PublicElementsFilter.ID
        )

        val TEST_DATA_PATH = System.getProperty("user.dir") + "/src/test/testData/"

    }

    @Parameterized.Parameter(0)
    @JvmField
    var filename: String = ""

    @Parameterized.Parameter(1)
    @JvmField
    var content: String? = null

    @Test
    fun test() {
        before()

        if (content != null) {
            myFixture.configureByText(filename, content!!)
        } else {
            runInEdtAndWait {
                myFixture.configureByFile(filename)
            }
        }

        doTest(
            ".tree"
        )
        doTest(
            ".inherited.tree", listOf(
                ExtSeeKotlinInheritedExtensionsNodeProvider.ID,
                ExtSeeJavaInheritedExtensionsNodeProvider.ID,
                InheritedMembersNodeProvider.ID
            )
        )
        doTest(
            ".inherited.inclNonPublic.tree", listOf(
                ExtSeeKotlinInheritedExtensionsNodeProvider.ID,
                ExtSeeJavaInheritedExtensionsNodeProvider.ID,
                InheritedMembersNodeProvider.ID
            ), listOf(
                PublicElementsFilter.ID
            )
        )


        if (filename.endsWith(".java")) {
            doTest(
                ".inherited.grouped.tree", listOf(
                    ExtSeeKotlinInheritedExtensionsNodeProvider.ID,
                    ExtSeeJavaInheritedExtensionsNodeProvider.ID,
                    InheritedMembersNodeProvider.ID,
                    SuperTypesGrouper.ID
                ), listOf(
                    PublicElementsFilter.ID
                )
            )
        }


    }

    private fun doTest(
        expectedFileSuffix: String, enabledActions: List<String> = listOf(), disabledActions: List<String> = listOf()
    ) {

        runInEdtAndWait {
            myFixture.testStructureView { structureView ->

                DEFAULT_ACTIONS.forEach {
                    structureView.setActionActive(it, true)
                }
                enabledActions.forEach {
                    structureView.setActionActive(it, true)
                }
                disabledActions.forEach {
                    structureView.setActionActive(it, false)
                }

                PlatformTestUtil.expandAll(structureView.tree)

                UsefulTestCase.assertSameLinesWithFile(
                    TEST_DATA_PATH + "results/" + filename + expectedFileSuffix,
                    PlatformTestUtil.print(structureView.tree, false),
                    true
                )

            }


        }
    }

    private var jdk: Sdk? = null

    private fun before() {
        try {
            super.setUp()
        } catch (ignored: IllegalStateException) {
            // TODO: Fix?
        }

        jdk = IdeaTestUtil.createMockJdk("java 1.8", TEST_DATA_PATH + "libs/mockJDK-1.8")

        runInEdtAndWait {
            runWriteAction {
                jdk?.let { jdk ->
                    ProjectJdkTable.getInstance().addJdk(jdk)
                    ModuleRootManager.getInstance(myFixture.module).modifiableModel.let {
                        it.sdk = jdk
                        it.commit()
                    }
                }
            }
        }

        myFixture.testDataPath = TEST_DATA_PATH

        myFixture.copyDirectoryToProject("project", "")
        PsiTestUtil.addLibrary(myFixture.module, "$TEST_DATA_PATH/libs/dummyLib.jar")

    }

    override fun tearDown() {

    }

}
