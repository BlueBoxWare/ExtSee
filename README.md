This plugin for IntelliJ and Android Studio adds Kotlin extension functions and extension properties which are defined
for a Kotlin or Java class to the [Structure View](https://www.jetbrains.com/help/idea/structure-tool-window-file-structure-popup.html) of that class.

> :warning: This plugin
> requires [K2 Mode](https://blog.jetbrains.com/idea/2024/08/meet-the-renovated-kotlin-support-k2-mode/) to be enabled
> in IntelliJ. To enable K2 mode, go to `Preferences/Settings` | `Languages & Frameworks` | `Kotlin` and tick the
`Enable K2 Kotlin mode` checkbox.

`public` extensions and `internal` extensions **defined in the project** are always shown. When `Show Non-public` is
enabled `internal` extensions **defined in libraries** are also shown. `private` extensions are never shown.

__Installation__: The plugin can be
found [in the JetBrains Plugins Repository](https://plugins.jetbrains.com/plugin/10346). To install from
IntelliJ/Android Studio: go to *Settings* -> *Plugins* -> *Browse repositories...* and search for "ExtSee". 

Plugin icon: copyright JetBrains s.r.o. and contributors, governed by the Apache 2.0 license. 

