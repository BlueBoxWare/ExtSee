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
package com.gmail.blueboxware.extsee.settings

import com.intellij.ide.DataManager
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.EdtExecutorService
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty0

internal class ExtSeeConfigurable(private val project: Project) : Configurable {

    private var component: DialogPanel? = null
    private val componentState: ExtSeeSettings.State = ExtSeeSettings.State()

    override fun getDisplayName() = "ExtSee"

    override fun isModified(): Boolean {
        component?.apply()
        var isModified = false
        process { configurable, component ->
            if (configurable.get() != component.get()) {
                isModified = true
                return@process
            }
        }
        return isModified
    }

    override fun apply() {
        component?.apply()
        process { configurable, component ->
            configurable.set(component.get())
        }
        UISettings.getInstance().fireUISettingsChanged()
    }

    override fun reset() {
        process { configurable, component ->
            component.set(configurable.get())
        }
        component?.reset()

    }

    private fun process(f: (KMutableProperty0<Boolean>, KMutableProperty0<Boolean>) -> Unit) {
        val configurable = project.service<ExtSeeSettings>()
        val map: Map<KMutableProperty0<Boolean>, KMutableProperty0<Boolean>> = mapOf(
            configurable::showExtensionsToolWindow to componentState::showExtensionsToolWindow,
            configurable::showExtensionsPopup to componentState::showExtensionsPopup,
            configurable::showInheritedToolWindow to componentState::showInheritedToolWindow,
            configurable::showInheritedPopup to componentState::showInheritedPopup,
            configurable::showOnAnyToolWindow to componentState::showOnAnyToolWindow,
            configurable::showOnAnyPopup to componentState::showOnAnyPopup
        )
        map.forEach { (p1, p2) ->
            f(p1, p2)
        }
    }

    override fun createComponent(): JComponent? {

        val findActionShortcut = ActionManager.getInstance().getKeyboardShortcut("GotoAction")?.let {
            "(" + KeymapUtil.getShortcutText(it) + ")"
        } ?: ""

        var openShortcutsRunnable: Runnable? = null

        component = panel {
            group("Structure Tool Window") {
                row {
                    checkBox("Show extensions").bindSelected(componentState::showExtensionsToolWindow)
                }
                row {
                    checkBox("Show extensions on superclasses").bindSelected(componentState::showInheritedToolWindow)
                }
                row {
                    checkBox("Show extensions inherited from Any").bindSelected(componentState::showOnAnyToolWindow)
                }
                row {
                    checkBox("Show extensions on companion objects").bindSelected(componentState::showOnCompanionToolWindow)
                }
            }
            group("Structure Popup") {
                row {
                    checkBox("Show extensions").bindSelected(componentState::showExtensionsPopup)
                }
                row {
                    checkBox("Show extensions on superclasses").bindSelected(componentState::showInheritedPopup)
                }
                row {
                    checkBox("Show extensions inherited from Any").bindSelected(componentState::showOnAnyPopup)
                }
                row {
                    checkBox("Show extensions on companion objects").bindSelected(componentState::showOnCompanionPopup)
                }
            }
            group("Tip") {
                row {
                    comment("<icon src='AllIcons.General.Information'>&nbsp; Use Find Action $findActionShortcut and search for 'ExtSee' to quickly toggle these options.")
                }
                row {
                    comment("<icon src='AllIcons.General.Information'>&nbsp; Assign keyboard shortcuts to the ExtSee toggle actions for even quicker access.")
                }
                row {
                    button("Configure Shortcuts") {
                        openShortcutsRunnable?.run()
                    }
                }
            }
        }

        openShortcutsRunnable = Runnable {
            Settings.KEY.getData(DataManager.getInstance().getDataContext(component))?.let { settings ->
                val configurable = settings.find("preferences.keymap")
                // https://github.com/JetBrains/intellij-community/blob/master/plugins/terminal/src/org/jetbrains/plugins/terminal/TerminalOptionsConfigurable.kt#L384
                settings.select(configurable, "ExtSee").doWhenDone {
                    EdtExecutorService.getScheduledExecutorInstance().schedule(
                        { settings.select(configurable, "ExtSee") }, 1000,
                        TimeUnit.MILLISECONDS
                    )
                }
            }
        }

        return component
    }

}
