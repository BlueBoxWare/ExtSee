fun JavaBaseClass.onJavaBaseClass() { }

fun JavaBaseClass?.onJavaBaseClassNullable() { }

fun JavaSubClass.onJavaSubClass() { }

fun JavaBaseInterface.onJavaBaseInterface() { }

fun JavaSubInterface.onJavaSubInterface() { }

fun JavaSubSubClass.onJavaSubSubClass() { }

fun JavaSubFromKotlin.onJavaSubFromKotlin() { }

fun KotlinBaseClass.onKotlinBaseClass() { }

fun KotlinBaseClass?.onKotlinBaseClassNullable() { }

fun KotlinSubClass.onKotlinSubClass() { }

fun KotlinBaseInterface.onKotlinBaseInterface() { }

fun KotlinSubInterface.onKotlinSubInterface() { }

fun KotlinSubSubClass.onKotlinSubSubClass() { }

fun KotlinSubFromJava.onKotlinSubFromJava() { }

internal fun JavaBaseClass.onJavaBaseClassInternal() { }

internal fun KotlinBaseClass.onKotlinBaseClassInternal() { }

private fun JavaBaseClass.onJavaBaseClassPrivate() { }

private fun KotlinBaseClass.onKotlinBaseClassPrivate() { }

fun JavaClass2.onJavaClass2() { }

fun KotlinClass2.onKotlinClass2() { }

fun Any.onAny() { }

fun Any?.onAnyNullable() { }



fun <X: KotlinSubClass, T: X>T.onKotlinSubClassGenerics()

fun <X: JavaSubClass, T: X>T.onJavaSubClassGenerics()

fun JavaContainer<*>.onJavaContainerAll()

fun KotlinContainer<*>.onKotlinContainerAll()