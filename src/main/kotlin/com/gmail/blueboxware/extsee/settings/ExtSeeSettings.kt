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

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State

@Service(Service.Level.PROJECT)
@State(name = "com.gmail.blueboxware.ExtSee")
class ExtSeeSettings : SerializablePersistentStateComponent<ExtSeeSettings.State>(State()) {

    var showExtensionsToolWindow: Boolean
        get() = state.showExtensionsToolWindow
        set(value) {
            updateState {
                it.copy(showExtensionsToolWindow = value)
            }
        }

    var showExtensionsPopup: Boolean
        get() = state.showExtensionsPopup
        set(value) {
            updateState {
                it.copy(showExtensionsPopup = value)
            }
        }

    var showInheritedToolWindow: Boolean
        get() = state.showInheritedToolWindow
        set(value) {
            updateState {
                it.copy(showInheritedToolWindow = value)
            }
        }

    var showInheritedPopup: Boolean
        get() = state.showInheritedPopup
        set(value) {
            updateState {
                it.copy(showInheritedPopup = value)
            }
        }

    var showOnAnyToolWindow: Boolean
        get() = state.showOnAnyToolWindow
        set(value) {
            updateState {
                it.copy(showOnAnyToolWindow = value)
            }
        }

    var showOnAnyPopup: Boolean
        get() = state.showOnAnyPopup
        set(value) {
            updateState {
                it.copy(showOnAnyPopup = value)
            }
        }

    var showOnCompanionToolWindow: Boolean
        get() = state.showOnCompanionToolWindow
        set(value) {
            updateState {
                it.copy(showOnCompanionToolWindow = value)
            }
        }

    var showOnCompanionPopup: Boolean
        get() = state.showOnCompanionPopup
        set(value) {
            updateState {
                it.copy(showOnCompanionPopup = value)
            }
        }

    data class State(
        var showExtensionsToolWindow: Boolean = true,
        var showExtensionsPopup: Boolean = false,

        var showInheritedToolWindow: Boolean = false,
        var showInheritedPopup: Boolean = false,

        var showOnAnyToolWindow: Boolean = true,
        var showOnAnyPopup: Boolean = true,

        var showOnCompanionToolWindow: Boolean = false,
        var showOnCompanionPopup: Boolean = false,
    )

}
