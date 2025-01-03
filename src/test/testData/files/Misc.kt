interface A
interface B

class C : A, B

fun <T> T.foo() where T : A, T : B {
}

open class Test

fun <X: T, T: Test>X.indirect() = ""

internal fun A.f(vararg a: Int) = 0

abstract class Level2<T: Comparable<T>>: Level1<T>()

abstract class Level4: Level3<Int>()

class Container {
    abstract inner class Inner1: Test() {
        inner class InnerInner1: Test()
    }
    class Inner2: Test() {
        class InnerInner2: Test()
        abstract inner class InnerInner3: Level2<Int>()
        interface InnerInterface: A
    }
}

data class MyFunction<T: Comparable<T>>(val i: Int): (T) -> String {
    override fun invoke(p1: T): String = ""
}

fun ((String) -> String).onMyFunction() = 3