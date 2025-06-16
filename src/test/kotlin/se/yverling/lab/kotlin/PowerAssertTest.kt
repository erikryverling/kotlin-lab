package se.yverling.lab.kotlin

import org.junit.Test

class PowerAssertTest {

    @Test
    fun `simple assert should fail`() {
        val hello = "Hello"
        val world = "world!"
        //assert(hello.length == world.substring(1, 4).length) { "Incorrect length" }
    }

    data class Person(val name: String, val age: Int)

    @Test
    fun `complex assert should fail`() {
        val person = Person("Alice", 10)
        //assert(person.name.startsWith("A") && person.name.length > 3 && person.age > 20 && person.age < 29)
    }
}
