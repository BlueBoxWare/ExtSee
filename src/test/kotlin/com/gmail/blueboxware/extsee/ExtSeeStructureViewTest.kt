package com.gmail.blueboxware.extsee

import com.gmail.blueboxware.extsee.settings.ExtSeeSettings
import com.intellij.ide.structureView.impl.java.JavaInheritedMembersNodeProvider
import com.intellij.ide.structureView.impl.java.PublicElementsFilter
import com.intellij.ide.structureView.impl.java.SuperTypesGrouper
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.common.ThreadLeakTracker
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
            "MyObject.java",
            "Collections.kt",
            "Generics.kt",
            "Misc.kt",
            "Properties.kt",
            "Generics.java",
            "CompanionObject.kt"
        )

        private val tests: Map<String, ExtSeeSettings.() -> Unit> = mapOf(
            "default" to { showInheritedToolWindow = false },
            "inherited" to {},
            "inherited_grouped" to {},
            "inherited_public_only" to {},
            "inherited_no_any" to { showOnAnyToolWindow = false },
            "not_companion" to { showOnCompanionToolWindow = false },
            "companion" to { showOnCompanionToolWindow = true },
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}: {3}")
        fun data(): List<Array<Any?>> = (javaSimpleTestCases.mapIndexed { index, content ->
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
        }).flatMap { inputs -> tests.map { (inputs + listOf(it.value) + it.key).toTypedArray() } }.filter {
            (it[0] as? String)?.endsWith(".kt") == false || !it[3]._contains("grouped")
        }
            .filter { it[0]._contains("CompanionObject") || !it[3]._contains("companion") }

        @Suppress("TestFunctionName")
        private fun Any?._contains(str: String) = (this as? String)?.contains(str) == true

        private val TEST_DATA_PATH = System.getProperty("user.dir") + "/src/test/testData/"

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
    var init: ExtSeeSettings.() -> Unit = {}

    @Parameterized.Parameter(3)
    @JvmField
    var testName: String = ""

    @Test
    fun test() {

        if (content != null) {
            myFixture.configureByText(filename, content ?: return)
        } else {
            runInEdtAndWait {
                myFixture.configureByFile(filename)
            }
        }

        project.service<ExtSeeSettings>().apply {
            showExtensionsToolWindow = true
            showInheritedToolWindow = true
            showOnAnyToolWindow = true
        }.init()

        runInEdtAndWait {
            myFixture.testStructureView { structureView ->

                structureView.setActionActive(Sorter.getAlphaSorterId(), true)

                structureView.setActionActive(PublicElementsFilter.ID, testName.contains("public_only"))
                structureView.setActionActive(SuperTypesGrouper.ID, testName.contains("grouped"))
                structureView.setActionActive(JavaInheritedMembersNodeProvider().name, testName.contains("inherited"))

                PlatformTestUtil.expandAll(structureView.tree)

                assertSameLinesWithFile(
                    "${TEST_DATA_PATH}results/$filename.${testName}",
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

        WriteCommandAction.runWriteCommandAction(project) {
            ProjectJdkTable.getInstance().addJdk(jdk ?: return@runWriteCommandAction, project)
            ModuleRootManager.getInstance(myFixture.module).modifiableModel.let {
                it.sdk = jdk
                it.commit()
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
        @Suppress("UnstableApiUsage") ThreadLeakTracker.longRunningThreadCreated(
            ApplicationManager.getApplication(), "SystemPropertyWatcher"
        )

        super.tearDown()
    }


}
