fun <T : Number> JavaGenerics.GenericClassOne<T>.onJGOne1() = 3
fun JavaGenerics.GenericClassOne<Number?>.onJGOne2() = 3
fun <T : Number> JavaGenerics.GenericClassOne<in T>.onJGOne3() = 3

fun JavaGenerics.GenericSubClassOne<Int>.onJGSubOne1() = 3

fun <T> JavaGenerics.GenericClassTwo<String, T & Any>.onJGTwo1() = 3
fun <T : Comparable<T>> JavaGenerics.GenericClassTwo<Any?, T>.onJGTwo2() = 3

fun <T : X, X : Int> JavaGenerics.GenericClassThree<T>.onJGThree1() = 3

fun JavaGenerics.GenericClassFour<out Int>.onJGFour1() = 3

fun JavaGenerics.GenericClassFive<Number, Int>.onJGFive1() = 3
fun JavaGenerics.GenericClassFive<Any?, Any>.onJGFive1() = 3

fun JavaGenerics.GenericClassSeven<String>.onJGSeven1() = 3
fun <T : String> JavaGenerics.GenericClassSeven<T>.onJGSeven2() = 3
fun <T> JavaGenerics.GenericClassSeven<T>.onJGSeven3()
        where T : CharSequence, T : Comparable<T> = 3

fun JavaGenerics.GenericClassEight<MutableList<String>>.onJGEight1() = 3

fun JavaGenerics.GenericClassThirteen<Int, MutableList<Int>>.onJGThirteen1() = 3
fun <T, U: List<T>> JavaGenerics.GenericClassThirteen<T, U>.onJGThirteen2() = 3
fun <T: Number, U: MutableList<T>> JavaGenerics.GenericClassThirteen<T, U>.onJGThirteen3() = 3

fun fff() {
    val v: JavaGenerics.GenericClassTwelve<Int>? = null
    v?.average()
}