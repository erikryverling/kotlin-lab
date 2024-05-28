@file:OptIn(ExperimentalContracts::class, ExperimentalContracts::class, ExperimentalContracts::class)

package se.yverling.lab.kotlin.kotlin

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
            if (it.name !in listOf(
                    "all",
                    "hashCode",
                    "toString",
                    "equals",
                    "component1",
                    "component2",
                ) && it.visibility == KVisibility.PUBLIC
            ) {
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

    interface Producer<out T> {
        fun get(): T
        // fun add(t: T) will cause compile error as T is declared as Out-bound.
        // This declaration means that Producer will only output T, never take it as an input.
        // The corresponding declaration or inputs would be <in T>.
    }

    fun `type projections`() {
        val ints: Array<Int> = arrayOf(1, 2, 3)
        val any = Array<Any>(3) { "" }
        copy(ints, any)
    }

    private fun copy(from: Array<out Any>, to: Array<Any>) {
        for (i in from.indices)
            to[i] = from[i]
        // from[i] = to[i] wil cause compile error as from is a Out-projected type
    }


    fun `generic function`() {
        println(singletonList("A")[0])
        println(singletonList(1)[0])
    }

    private fun <T> singletonList(item: T): List<T> {
        return listOf(item)
    }

    fun `generic extension method`() {
        println(1.hash())
        println("A".hash())
    }

    private fun <T> T.hash(): Int? {
        return this?.hashCode()
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

    private infix fun String.zip(other: String): String {
        return "${this[0]}${other[0]}${this[1]}${other[1]}${this[2]}${other[2]}"
    }

    fun `tail recursion`() {
        countDown(10)
        println()
    }

    // This will make the Kotlin compiler generate an optimized loop version
    private tailrec fun countDown(n: Int) {
        if (n == 0) return
        print("$n ")
        countDown(n - 1)
    }


    fun `reified and infix`() {
        data class Car(val doors: Int)

        val string = membersOf<Car>().joinToString(", ")
        println(string)
    }

    private inline fun <reified T> membersOf() = T::class.members.map { it.name }

    fun contracts() {
        val nullableString: String? = null

        if (notNullOrEmpty(nullableString)) {
            println(nullableString.length)
        }
    }

    private fun notNullOrEmpty(nullableString: String?): Boolean {
        contract {
            returns(true) implies (nullableString != null)
        }

        return nullableString != null && nullableString != ""
    }

    fun `class delegation`() {
        val baseData = ProfileData("John", "Doe", 34)
        val student = Student(baseData, "KTH")

        println(student.name)
    }

    interface Profile {
        val name: String
        val surname: String
        val age: Int
    }

    data class ProfileData(
        override val name: String,
        override val surname: String,
        override val age: Int
    ) : Profile

    data class Student(
        val data: ProfileData,
        val university: String
    ) : Profile by data


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

    // Classes are final by default. To make them extendable add the 'open' modifier
    open class Vehicle(
        val name: String
    )

    data class Car(val doors: Int) : Vehicle(name = "Car")

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

    fun `builders`() {
        val html = html {
            head {
                title { +"HTML builder example" }
            }
            body {
                h1 { +"Builders" }
                p { +"They are great" }
            }
        }
        println(html)

        val s = "String"

        // Type is ItemHolder<String>
        itemHolderBuilder {
            addItem(s)
        }
        // Type is ItemHolder<String>
        itemHolderBuilder {
            addAllItems(listOf(s))
        }
        // Type is ItemHolder<String?>
        itemHolderBuilder {
            val lastItem: String? = getLastItem()
            println(lastItem)
        }
    }

    fun `destructing declarations`() {
        val person = Person(name = "Ian", age = 86)
        // Note! age is ignored
        val (name, _) = person
        print("Name: $name")
    }

    fun `smart cast in Kotlin 2`() {
        petAnimal(Cat("Meowth"))

        signalCheck(object : Postponed {
            override fun signal() {
                println("Postponed")
            }
        })

        runProcessor(object : Processor {
            override fun process() {
                println("Processing")
            }

        })

        Holder {
            println("Holding on")
        }.process()
    }


    private interface Status {
        fun signal()
    }

    private interface Ok : Status
    private interface Postponed : Status
    private interface Declined : Status


    private fun petAnimal(animal: Any) {
        val isCat = animal is Cat
        if (isCat) {
            // In Kotlin 2.0.0-Beta5, the compiler can access
            // information about isCat, so it knows that
            // animal was smart cast to type Cat.
            // Therefore, the purr() function is successfully called.
            // In Kotlin 1.9.20, the compiler doesn't know
            // about the smart cast, so calling the purr()
            // function triggers an error.
            animal.purr()
        }
    }

    private fun signalCheck(signalStatus: Any) {
        if (signalStatus is Postponed || signalStatus is Declined) {
            // signalStatus is smart cast to a common supertype Status
            signalStatus.signal()
            // Prior to Kotlin 2.0.0-Beta5, signalStatus is smart cast
            // to type Any, so calling the signal() function triggered an
            // Unresolved reference error. The signal() function can only
            // be called successfully after another type check:

            // check(signalStatus is Status)
            // signalStatus.signal()
        }
    }

    private interface Processor {
        fun process()
    }

    private inline fun inlineAction(f: () -> Unit) = f()

    private fun nextProcessor(): Processor? = null

    private fun runProcessor(processor: Processor?): Processor? {
        var processor: Processor? = processor
        inlineAction {
            // In Kotlin 2.0.0-Beta5, the compiler knows that processor
            // is a local variable, and inlineAction() is an inline function, so
            // references to processor can't be leaked. Therefore, it's safe
            // to smart cast processor.

            // If processor isn't null, processor is smart cast
            if (processor != null) {
                // The compiler knows that processor isn't null, so no safe call
                // is needed
                processor.process()

                // In Kotlin 1.9.20, you have to perform a safe call:
                // processor?.process()
            }

            processor = nextProcessor()
        }

        return processor
    }

    private class Holder(val provider: (() -> Unit)?) {
        fun process() {
            // In Kotlin 2.0.0-Beta5, if provider isn't null, then
            // provider is smart cast
            if (provider != null) {
                // The compiler knows that provider isn't null
                provider()

                // In 1.9.20, the compiler doesn't know that provider isn't
                // null, so it triggers an error:
                // Reference has a nullable type '(() -> Unit)?', use explicit '?.invoke()' to make a function-like call instead
            }
        }
    }

    // -- Sealed class --

    private sealed class Expr {
        data class Const(val number: Double) : Expr()
        data class Sum(val e1: Expr, val e2: Expr) : Expr()
        data object NotANumber : Expr()
    }

    private fun eval(expr: Expr): Double = when (expr) {
        is Expr.Const -> expr.number
        is Expr.Sum -> eval(expr.e1) + eval(expr.e2)
        Expr.NotANumber -> Double.NaN
    }

    // -- Common --

    class Person(var name: String, var age: Int) {
        fun capitalizedName(): String {
            return name.capitalize()
        }
    }

    operator fun Person.component1() = name
    operator fun Person.component2() = age

    @JvmInline
    value class Cat(private val name: String) {
        init {
            require(name.matches(Regex("[A-Za-z]+"))) { "Cat name only contain letter" }
        }

        fun purr() {
            println("Purr purr")
        }
    }
}
