open class G<T: Any>

open class GNumber: G<Number>()

open class GInt: G<Int>()

open class GString: G<String>()

open class GNumber2<T: Number>: G<T>()

open class P<T: Any, U: Any>

open class PIntInt: P<Int, Int>()

open class PInt<T: Any>: P<Int, T>()

fun <T: Any>G<T>.onG_any() {}
fun G<in Int>.onG_inInt() { }
fun <T: Int>G<T>.onG_int2() { }
fun <X: String, T: X>G<T>.onG_string() {}
fun G<Number>.onG_number() { }
fun G<out Number>.onG_outNumber() { }
fun P<Int, String>.onP_intString() { }
fun <T: Number>P<T, String>.onP_outNumberString() { }
fun G<()->Boolean>.onG_fun() { }

typealias Alias1 = G<Number>
typealias Alias2<T> = P<Int, T>
typealias Alias3 = Alias2<String>

class FA: Alias1()
class FA2: Alias2<String>()
class FA3: Alias3()

class F<T: ()-> Any>

fun F<()->Boolean>.onF() { }

open class Multi<E, T, S: Collection<E>>

class Sub1<T>: Multi<Int, T, Collection<Int>>()
class Sub2: Multi<Any?, Int, Collection<Any?>>()
class Sub3: Multi<Any, Int, List<Int>>()

fun <T: Any> Multi<T, Int, Collection<T>>.onMulti() = 3
fun Multi<Int, Int, Collection<Int>>.onMulti2() = 3
fun <T: Number> Multi<Int, T, Collection<Int>>.onMulti3() = 3

interface CollectionInt: Collection<Int>