import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JavaGenerics {

    public static class GenericClassOne<T> {
    }

    public static class GenericSubClassOne<T extends Number> extends GenericClassOne<T> {
    }

    public static class GenericClassTwo<K, V> {
    }

    public static class GenericClassThree<T extends Number> {

    }

    public static class GenericClassFour<T extends Comparable<T>> {

    }

    public static class GenericClassFive<T, U extends T> {

    }

    public static class NestedGenericClass<T> {
        public static class InnerGenericClass<U> {

        }
    }

    public static class GenericClassSeven<T extends CharSequence & Comparable<T>> {
    }

    public static class GenericClassEight<T extends List<String>> {
    }

    public static class GenericClassNine<T extends Map<String, Integer>> {
    }

    public static class GenericClassTen<T extends Set<Double>> {
    }

    public static class GenericClassEleven<T extends Runnable> {
    }

    public static abstract class GenericClassTwelve<T extends Serializable> implements List<Integer> {
    }

    public static class GenericClassThirteen<T, U extends List<T>> {

    }


}