open class G<T: Any>

open class GNumber: G<Number>()

open class GInt: G<Int>()

open class GString: G<String>()

open class GNumber2<T: Number>: G<T>()

open class P<T: Any, U: Any>

open class PIntInt: P<Int, Int>()

open class PInt<T: Any>: P<Int, T>()

val <T: Any>G<T>.onG_any: Int
    get() = 3
val G<in Int>.onG_inInt: Int
    get() = 3
val <T: Int>G<T>.onG_int2: Int
    get() = 3
val <X: String, T: X>G<T>.onG_string: Int
    get() = 3
val G<Number>.onG_number: Int
    get() = 3
val G<out Number>.onG_outNumber: Int
    get() = 3
val P<Int, String>.onP_intString: Int
    get() = 3
val <T: Number>P<T, String>.onP_outNumberString: Int
    get() = 3
val G<()->Boolean>.onG_val: Int
    get() = 3

typealias Alias1 = G<Number>
typealias Alias2<T> = P<Int, T>
typealias Alias3 = Alias2<String>

class FA: Alias1()
class FA2: Alias2<String>()
class FA3: Alias3()

class F<T: ()-> Any>

val F<()->Boolean>.onF: Int
    get() = 3

open class Multi<E, T, S: Collection<E>>

class Sub1<T>: Multi<Int, T, Collection<Int>>()
class Sub2: Multi<Any?, Int, Collection<Any?>>()
class Sub3: Multi<Any, Int, List<Int>>()

val <T: Any> Multi<T, Int, Collection<T>>.onMulti: Int
    get() = 3
val Multi<Int, Int, Collection<Int>>.onMulti2: Int
    get() = 3
val <T: Number> Multi<Int, T, Collection<Int>>.onMulti3: Int
    get() = 3