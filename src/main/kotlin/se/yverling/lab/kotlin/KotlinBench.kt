@file:OptIn(ExperimentalContracts::class, ExperimentalContracts::class, ExperimentalContracts::class)

package se.yverling.lab.kotlin

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KVisibility

typealias StringIntMap = Map<String, Int>

internal object KotlinBench {
    fun all() {
        KotlinBench::class.members.forEach {
            if (it.name !in listOf("all", "hashCode", "toString", "equals") && it.visibility == KVisibility.PUBLIC) {
                println("--- ${it.name} ---")
                it.call(KotlinBench)
                println()
            }
        }
    }

    fun with() {
        val person = Person("Mary", 42)

        // "with" this object do the following
        with(person) {
            println("Age: $age")
        }
    }

    fun `apply and also`() {
        val person = Person("Mary", 42)

        // "apply" the following things to this object and "also" do this (that doesn't alter the object)
        person.apply {
            age = 20
        }.also {
            println("Age: ${it.age}")
        }
    }

    fun let() {
        val person = Person("Mary", 42)

        // Used to introduce a new scoped lambda param
        person.let { mary ->
            println("Name: ${mary.name}")
        }
    }

    fun run() {
        val person = Person("Mary", 42)

        // Used to apply and also call functions on an object
        person.run {
            name = "martin"
            println("Name: ${capitalizedName()}")
        }
    }

    fun producer() {
        val producer: Producer<Any>

        producer = object : Producer<String> {
            override fun get(): String {
                return "Produced value with String type"
            }
        }

        println(producer.get())
    }

    fun `type projections`() {
        val ints: Array<Int> = arrayOf(1, 2, 3)
        val any = Array<Any>(3) { "" }
        copy(ints, any)
    }

    fun `generic function`() {
        println(singletonList("A")[0])
        println(singletonList(1)[0])
    }

    fun `generic extension method`() {
        println(1.hash())
        println("A".hash())
    }

    fun `type alias`() {
        val map: StringIntMap = mapOf("A" to 1, "B" to 2)
        println(map["A"])
        println(map["B"])
    }

    fun `operator overload`() {
        data class Engine(val effect: Int) {

            operator fun plus(other: Engine): Engine {
                return Engine(this.effect + other.effect)
            }

            operator fun compareTo(other: Engine): Int {
                return this.effect.compareTo(other.effect)
            }
        }

        val twinEngine: Engine = Engine(5) + Engine(5)
        println("Twin engine effect: ${twinEngine.effect}")
    }

    fun deconstructing() {
        // The data class will automatically generate componentN() functions used for deconstruction
        data class Car(val effect: Int, val numberOfDoors: Int, val stationWagon: Boolean)

        val standardCar = Car(90, 4, false)
        val (effect, _, stationWagon) = standardCar

        println("Effect: $effect")
        println("Is a station wagon: $stationWagon")
    }

    fun infix() {
        println(("cow" zip "car"))
    }

    fun `tail recursion`() {
        countDown(10)
        println()
    }

    fun `reified and infix`() {
        data class Car(val doors: Int)

        val string = membersOf<Car>().joinToString(", ")
        println(string)
    }

    fun contracts() {
        val nullableString: String? = null

        if (notNullOrEmpty(nullableString)) {
            println(nullableString.length)
        }
    }

    fun `class delegation`() {
        val baseData = PersonData("John", "Doe", 34)
        val student = Student(baseData, "KTH")

        println(student.name)
    }

    fun `property delegation`() {
        val greetingAsLazy: Lazy<String> = lazy { "Hello" }
        println("${greetingAsLazy.value}, World!")

        // Using the "by" keyword we can skip using .value and access the value property directly
        val greetingAsString: String by lazy { "Hello" }
        println("$greetingAsString, World!")

        /* Note that greetingAsString is connected to the Lazy.value property and not just a String
            variable. This could easily be verified by trying to make greetingAsStringButReallyItsLazy
            a var. This won't work in this case since Lazy.value doesn't have a set() method.

           Try it yourself by uncommenting the line below:
         */
        // var greetingAsStringButReallyItsLazy: String by lazy { "Hello" }
    }

    fun `list copy`() {
        val originalList = listOf(1, 2, 3)
        val ordinalListPlusOne = originalList.toMutableList().apply {
            add(1, 4)
        }
        println(ordinalListPlusOne.size)
    }

    fun `immutable collections`() {
        /*
            A mutable list allows full modifications of its elements and has an mutable underlying
            data structure
         */
        val mutableList = mutableListOf(1, 2, 3)

        mutableList.removeAt(1)
        println("Mutable list size: ${mutableList.size}")

        /*
            A read-only list provides an interface that doesn't allow modifications of its
            elements, but is still implemented with an underlying mutable data structure that could
            be modified.
         */
        val readOnlyList = listOf(1, 2, 3)

        (readOnlyList as MutableList<Int>)[1] = -1
        println("Second read-only list element: ${readOnlyList[1]}")

        /*
            A persistent list is allowing modifications of its element, but is implemented with an
            immutable underlying data structure
         */
        val persistentList = persistentListOf(1, 2, 3)

        persistentList.removeAt(1)
        println("Persistent list size: ${persistentList.size}")

        try {
            (persistentList as MutableList<Int>)[1] = -1
        } catch (e: Exception) {
            println("Caught exception: ${e.message}")
        }

        /*
            An immutable list provides an interface that doesn't allow modifications of its
            elements and is implemented with immutable underlying data structure.
            It's thus truly immutable.
         */
        val immutableList: ImmutableList<Int> = persistentList.toImmutableList()
        try {
            (immutableList as MutableList<Int>)[1] = -1
        } catch (e: Exception) {
            println("Caught exception: ${e.message}")
        }
    }

    fun `minOf() and minBy()`() {
        val cars = listOf(Car(1), Car(2), Car(3))

        println(cars.minOf { it.doors })
        println(cars.minByOrNull { it.doors })
    }

    @Suppress("UNREACHABLE_CODE")
    fun `guard for property`() {
        val property = null

        val otherProperty = property ?: return
    }

    fun `null-safe call`() {
        val person: Person? = null
        println("Person.name = ${person?.name}")
    }

    fun `nullable as`() {
        val map = emptyMap<String, Any>()

        val value = map["test"] as? String
        println("Test: $value")
    }

    fun `value class`() {
        Cat("Luna")

        try {
            Cat("123")
        } catch (e: Throwable) {
            println("Caught exception: ${e.message}")
        }
    }

    fun `throw if null`() {
        val nullableCar: Car? = null
        try {
            nullableCar?.doors ?: throw IllegalStateException("Cat can't be null!")
        } catch (e: IllegalStateException) {
            println("Caught exception: ${e.message}")
        }
    }

    fun `nothing`() {
        val nullableCar: Car? = null
        try {
            nullableCar?.doors ?: fail("Cat can't be null!")
        } catch (e: IllegalStateException) {
            println("Caught exception: ${e.message}")
        }
    }

    /*
     * The throw expression has the type Nothing.
     * This type has no values and is used to mark code locations that can never be reached.
     */
    private fun fail(message: String): Nothing {
        throw IllegalStateException(message)
    }

    // Scoped functions
    class Person(var name: String, var age: Int) {
        fun capitalizedName(): String {
            return name.capitalize()
        }
    }

    // Sealed class

    sealed class Expr {
        data class Const(val number: Double) : Expr()
        data class Sum(val e1: Expr, val e2: Expr) : Expr()
        data object NotANumber : Expr()
    }

    private fun eval(expr: Expr): Double = when (expr) {
        is Expr.Const -> expr.number
        is Expr.Sum -> eval(expr.e1) + eval(expr.e2)
        Expr.NotANumber -> Double.NaN
    }

    // Generics

    interface Producer<out T> {
        fun get(): T
        // fun add(t: T) will cause compile error as T is declared as Out-bound
    }

    private fun copy(from: Array<out Any>, to: Array<Any>) {
        for (i in from.indices)
            to[i] = from[i]
        // from[i] = to[i] wil cause compile error as from is a Out-projected type
    }

    private fun <T> singletonList(item: T): List<T> {
        return listOf(item)
    }

    private fun <T> T.hash(): Int? {
        return this?.hashCode()
    }

    // Infix

    private infix fun String.zip(other: String): String {
        return "${this[0]}${other[0]}${this[1]}${other[1]}${this[2]}${other[2]}"
    }

    // Tail recursion

    // This will make the Kotlin compiler generate an optimized loop version
    private tailrec fun countDown(n: Int) {
        if (n == 0) return
        print("$n ")
        countDown(n - 1)
    }

    // Inline and reified
    private inline fun <reified T> membersOf() = T::class.members.map { it.name }
}

// Contracts

fun notNullOrEmpty(nullableString: String?): Boolean {
    contract {
        returns(true) implies (nullableString != null)
    }

    return nullableString != null && nullableString != ""
}

// Class delegates

interface Person {
    val name: String
    val surname: String
    val age: Int
}

data class PersonData(
    override val name: String,
    override val surname: String,
    override val age: Int
) : Person

data class Student(
    val data: PersonData,
    val university: String
) : Person by data


// Misc

// Classes are final by default. To make them extendable add the 'open' modifier
open class Vehicle(
    val name: String
)

data class Car(val doors: Int) : Vehicle(name = "Car")

@JvmInline
value class Cat(private val name: String) {
    init {
        require(name.matches(Regex("[A-Za-z]+"))) { "Cat name only contain letter" }
    }
}
