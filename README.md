This plugin for IntelliJ and Android Studio adds Kotlin extension functions and extension properties which are defined
for a Kotlin or Java class to the [Structure View](https://www.jetbrains.com/help/idea/structure-tool-window-file-structure-popup.html) 
of that class.

Only top level `public` and `internal` extensions defined in the current project and top level `public` 
extensions defined in libraries are shown. When you enable the _Show non-public_ option [item 3 in the table below] `internal` extensions from libraries are shown too.

`Private` and local extensions are never shown (at least not as extensions, they still appear as normal definitions in the
Structure View of the file they are defined in, as do all extensions: this plugin does not remove any items from the Structure View).

__Installation__: The plugin can be found [in the JetBrains Plugins Repository](https://plugins.jetbrains.com/plugin/10346). To install from IntelliJ/Android Studio: go to *Settings* -> *Plugins* -> *Browse repositories...* and search for "ExtSee". 

<div align="center">
<img src="images/image.png" />
</div>

|              |                                              |                      |
|--------------|----------------------------------------------|----------------------|
| <kbd>1</kbd> | ![extension](images/extensions.png)          | Show/hide extensions |
| <kbd>2</kbd> | ![inherited extension](images/inherited.png) | Show/hide extensions defined on superclasses and interfaces |
| <kbd>3</kbd> | ![lock](images/private_boxed.png)            | When enabled: show ``internal`` extensions defined inside libraries too |
