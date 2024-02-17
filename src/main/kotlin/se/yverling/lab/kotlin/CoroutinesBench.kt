@file:OptIn(ExperimentalCoroutinesApi::class)

package se.yverling.lab.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resumeWithException
import kotlin.system.measureTimeMillis


/*
    A coroutine is an instance of suspendable computation.
    It's conceptually similar to a thread, in the sense that it
    takes a block of code to run that works concurrently with the
    rest of the code. However, a coroutine is not bound to any particular thread.
    It may suspend its execution in one thread and resume in another one.

    Coroutines can be thought of as light-weight threads, but there is a number of important
    differences that make their real-life usage very different from threads.
 */
object CoroutinesBench {
    fun all() {
        `Launch a simple coroutine`()
        `Launch a simple coroutine using an a job and wait()`()
        `Launch two coroutines in a new scope`()
        `Launch coroutine as a job`()
        `Launch a huge amount of coroutines`()
        `Canceling a coroutine`()
        `Coroutine with timeout`()
        `Running suspended functions sequentially`()
        `Running suspended functions concurrently with async() & await()`()
        `Structured concurrency with async`()
        `Coroutine dispatchers`()
        `Simple Channel example`()
        `Using the producer-consumer pattern with Channel`()
        `Nested coroutines`()
        suspendCancellableCoroutine()
    }

    /**
     * See also [CoroutinesTest]
     */
    fun `Launch a simple coroutine`() {
        /*
        All coroutines much be executed in a CoroutineScope.
        A scope is the main holder for coroutines and contains
        specific things like a context, a dispatcher and a job (see below).

        runBlocking() creates a CoroutineScope and will use a Dispatchers.Unconfined, if no other dispatcher is configured explicitly.

        runBlocking() keeps track of the coroutines that are queued on the scheduler
        used by the dispatcher of its CoroutineScope,
        and will not return as long as there’s pending work on that scheduler.

        So by default it's:

        runBlocking() -> CoroutineScope -> Dispatchers.Unconfined -> CoroutineScheduler

         */
        runBlocking {
            // Launches a new coroutine and continues on main execution path
            launch {
                println("Coroutine: In launch()")

                // The execution will suspend at this point and return to the main execution path
                printDelayed()
            }

            println("Main: Done")
        }
    }


    fun `Launch a simple coroutine using an a job and wait()`() = runBlocking {
        val job = launch {
            println("Coroutine: In launch()")
            printDelayed()
        }

        /*
         * This will explicitly wait for the coroutine belonging to job to terminate.
         * It resembles of the advanceUntilIdle() using while testing.
         */
        job.join()

        println("Main: Done")
    }
}

fun `Launch two coroutines in a new scope`() = runBlocking {
    /*
    A coroutine scope waits until it's containing coroutines completes.
    In contrast to runBlocking() it does not block, but suspends execution.
     */
    coroutineScope {
        launch {
            printDelayed()
        }

        // The second coroutine will be executed concurrently with the first one
        launch {
            printDelayed(id = 2)
        }

        println("Main: Coroutine scope has launched")
    }

    println("Main: Coroutine scope is done")
}


fun `Launch coroutine as a job`() = runBlocking {
    // The result type of launch() is Job
    val job = launch {
        printDelayed()
    }

    println("Main: Waiting for job to finish")

    // Wait for Job to finish
    job.join()

    println("Main: Done waiting")
}


fun `Launch a huge amount of coroutines`() = runBlocking {
    /*
    Coroutines are lightweight which means that you could launch
    100 000 of them with minimal CPU and memory usage.
     */
    repeat(100_000) {
        launch {
            printDelayed(id = it)
        }
    }
}

fun `Canceling a coroutine`() = runBlocking {
    val job = launch {
        while (true) {
            printDelayed(100)
        }
    }

    delay(3000)
    println("Main: Canceling coroutine")

    /*
    Coroutine cancellation is cooperative.
    A coroutine has to cooperate to be cancellable.
    All the suspending functions in kotlinx.coroutines are cancellable.
    They check for cancellation of coroutine and throw CancellationException when cancelled.
     */
    job.cancel()

    job.join()
    println("Main: Cancel done")
}


fun `Coroutine with timeout`() = runBlocking {
    /*
    Launch and monitor a coroutine with a specified timeout value.
    When the timeout value is overridden the coroutine is canceled.
     */
    try {
        withTimeout(1000) {
            while (true) {
                printDelayed(300)
            }
        }
    } catch (e: TimeoutCancellationException) {
        println("Got timeout")
    }
}


fun `Running suspended functions sequentially`() = runBlocking {
    val time = measureTimeMillis {
        // Code inside coroutines are executed sequentially
        println(calculatePartOne() + calculatePartTwo())
    }
    println("Completed in $time ms")
}


fun `Running suspended functions concurrently with async() & await()`() = runBlocking {
    val time = measureTimeMillis {
        val partOne = async { calculatePartOne() }
        val partTwo = async { calculatePartTwo() }
        /*
        Using async() and await() the suspended method
        are run concurrently and could return in any order.
        This make execution much faster than sequential code.

        The result type of async() is Deferred. Calling await() on
        a Deferred returns a value, in contrast to Job that doesn't return a value.
         */
        println(partOne.await() + partTwo.await())
    }
    println("Completed in $time ms")
}


fun `Structured concurrency with async`() = runBlocking {
    val time = measureTimeMillis {
        println(calculateSumOfOneAndTwo())
    }
    println("Completed in $time ms")
}

fun `Coroutine dispatchers`() = runBlocking {
    /*
    If no dispatcher is defined the coroutine will use the parent context and dispatcher
    . This is also called a confined dispatcher.
     */
    launch {
        println("Confined dispatcher: I'm working in thread ${Thread.currentThread().name}")
        delay(1000)
        println("Confined dispatcher: After delay in thread ${Thread.currentThread().name}")
    }

    /*
    An Unconfined dispatcher will start with the parent dispatcher, but then switch
    to the Default dispatcher after first suspension.
     */
    launch(Dispatchers.Unconfined) {
        println("Unconfined dispatcher: I'm working in thread ${Thread.currentThread().name}")
        delay(500)
        println("Unconfined dispatcher: After delay in thread ${Thread.currentThread().name}")
    }

    /*
    The Default dispatcher will use a custom thread pool for running the coroutine
     */
    launch(Dispatchers.Default) {
        println("Default dispatcher: I'm working in thread ${Thread.currentThread().name}")
        delay(500)
        println("Default dispatcher: After delay in thread ${Thread.currentThread().name}")
    }
}

/*
 * Channels are a way to send (or stream) values between coroutines.
 * In contrast to Flows Channels are hot.
 */
fun `Simple Channel example`() = runBlocking {
    val channel = Channel<Int>()

    launch {
        for (i in 1..5) channel.send(i)
        channel.close()
    }

    for (i in channel) println(i)

    println("Done!")
}

/*
 * This is an example of using the producer-consumer pattern using Channel.
 * There are other use cases that could be interesting such as fan-out, fan-in and buffered Channels.
 */
fun `Using the producer-consumer pattern with Channel`() = runBlocking {
    val numbers = produceNumbers()

    // In this case we are using a so-called pipeline, to route a Channel trough another Channel
    val squares = squareNumbers(numbers)

    for (i in squares) println(i)

    println("Done!")
}

fun `Nested coroutines`() = runBlocking {
    coroutineScope {
        launch {// Coroutine #3
            println("1: ${Thread.currentThread().name}")

            launch {// Coroutine #4
                println("2: ${Thread.currentThread().name}")
            }

            launch {// Coroutine #5
                withContext(Dispatchers.IO) {
                    println("3: ${Thread.currentThread().name}")
                }

                launch(Dispatchers.Default) {// Coroutine #6
                    println("4: ${Thread.currentThread().name}")
                }

                println("5: ${Thread.currentThread().name}")
            }

            println("5: ${Thread.currentThread().name}")
        }
    }
}

fun suspendCancellableCoroutine() = runBlocking {
    val greeting = suspendCancellableCoroutine { continuation ->
        val callback = object : Listener {
            override fun onSuccess(message: String) {
                // This will make sure to only return one value.
                // Otherwise, continuation will throw an exception
                if (continuation.isActive) {
                    continuation.resume(message, onCancellation = null)
                }
            }

            override fun onError(message: String) {
                if (continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException(message))
                }
            }
        }

        Producer.register(callback)
    }
    println(greeting)
}

// The "suspend" modifier is used when a function contains a suspending call, such as delay
internal suspend fun printDelayed(delayInMillis: Long = 1000, id: Int = 1) {
    delay(delayInMillis)
    println("Coroutine #$id has delayed $delayInMillis ms")
}

private interface Listener {
    fun onSuccess(message: String)
    fun onError(message: String)
}

private object Producer {
    fun register(listener: Listener) {
        listener.onSuccess("Hello, World!")
        listener.onError("ERROR!") // This won't show
    }
}

/*
This method uses structured concurrency using coroutineScope(), which means that if an
exception occurs in it all launched coroutines will be guaranteed to be canceled
 */
private suspend fun calculateSumOfOneAndTwo(): Int = coroutineScope {
    val partOne = async { calculatePartOne() }
    val partTwo = async { calculatePartTwo() }

    partOne.await() + partTwo.await()
}

private suspend fun calculatePartOne(): Int {
    delay(1000)
    return 47
}

private suspend fun calculatePartTwo(): Int {
    delay(1000)
    return 11
}

private fun CoroutineScope.produceNumbers(): ReceiveChannel<Int> = produce {
    for (i in 1..5) send(i)
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun CoroutineScope.squareNumbers(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> =
    produce {
        for (i in numbers) send(i * i)
    }