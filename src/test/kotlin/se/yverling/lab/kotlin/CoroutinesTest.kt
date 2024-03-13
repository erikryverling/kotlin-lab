package se.yverling.lab.kotlin

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.Test
import se.yverling.lab.kotlin.coroutines.printDelayed

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutinesTest {

    @Test
    fun `Launch a simple coroutine`() {
        /*
        All coroutines much be executed in a CoroutineScope.
        A scope is the main holder for coroutines and contains
        specific things like a context, a dispatcher and a job (see below).

        runTest() is a scope used for testing. It will automatically skip any delays in coroutines,
        making the test above complete much faster than one second.

        runTest() creates a TestScope and will use a StandardTestDispatcher, if no other dispatcher is configured explicitly.

        runTest() keeps track of the coroutines that are queued on the scheduler used by the dispatcher of its TestScope,
        and will not return as long as there’s pending work on that scheduler.

        So by default it's:

        runTest() -> TestScope -> StandardTestDispatcher -> TestCoroutineScheduler

        There's also a runBlockingTest() scope used for blocking. Note! You should only call runTest() once in a test.
         */
        runTest {
            // Launches a new coroutine and continues on main execution path
            launch {
                println("Coroutine: In launch()")

                // The execution will suspend at this point and return to the main execution path
                printDelayed()
            }

            /*
             * By default, the StandardTestDispatcher run all launched coroutines in the TestScope after the main test coroutine
             * is done, but before runTest() returns. By calling advanceUntilIdle() the test coroutine will wait until all launched
             * coroutines are done, and then continue.
             */
            advanceUntilIdle()

            println("Main: Done")
        }
    }

    @Test
    fun `Launch a simple coroutine using an UnconfinedTestDispatcher`() {
        /*
        Instead of calling advanceUntilIdle() we could use UnconfinedTestDispatcher instead of StandardTestDispatcher.
        This dispatcher will start the launched coroutines eagerly. This means that they’ll start running immediately,
        without waiting for their coroutine builder to return. This will make it easier to test, but it will differ from
        the production code as normal dispatcher acts more like  StandardTestDispatcher to promote concurrency.
         */
        runTest(UnconfinedTestDispatcher()) {
            /*
             * Note that the reason this coroutine is launched eagerly is because it will inherit UnconfinedTestDispatcher,
             * as this is the standard behaviour, unless another dispatcher is explicitly declared.
             */
            launch {
                println("Coroutine: In launch()")

                /*
                 * Note that printDelayed() is printed out after "Main: Done".
                 * UnconfinedTestDispatcher starts new coroutines eagerly, but that doesn’t mean that it’ll run them to
                 * completion eagerly as well. If a launched coroutine suspends, other coroutines will resume executing.
                 */
                printDelayed()
            }
            println("Main: Done")
        }
    }

    @Test
    fun `Injecting a Dispatcher and using async() await()`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        /*
         * By injecting a StandardTestDispatcher the delay() inside fetchData() will be skipped and it won't switch thread.
         */
        val repository = TestRepository(testDispatcher)

        /*
         * initialize() uses async() to let test (or other callers) wait for it to complete before doing anything else.
         * The alternative could be to not use it and then instead having the test to call advanceUntilIdle(). This is
         * not preferable in tests, and also not possible in production code.
         */
        repository.initializeAsync().await()

        repository.initialized.get().shouldBeTrue()

        val data = repository.fetchData()
        data.shouldBe("Hello world")
    }

    @Test
    fun `Use a custom scope to simulate a lifecycle using the main thread`() = runTest {
        val testDispatcher = StandardTestDispatcher()

        // As Lifecycle operates on the Main thread using MainScope, we need to override it with StandardTestDispatcher
        Dispatchers.setMain(testDispatcher)

        try {
            println("Main: Creating lifecycle")
            val lifecycle = Lifecycle()

            println("Main: Launching coroutines")
            lifecycle.launchCoroutines()
            delay(3000)

            println("Main: Destroying lifecycle")
            lifecycle.destroy()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

internal class TestRepository(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val scope = CoroutineScope(ioDispatcher)
    val initialized = AtomicBoolean(false)

    // A function that starts a new coroutine on the IO dispatcher
    fun initializeAsync() = scope.async {
        initialized.set(true)
    }

    // A suspending function that switches to the IO dispatcher
    suspend fun fetchData(): String = withContext(ioDispatcher) {
        require(initialized.get()) { "Repository should be initialized first" }
        delay(5000L)
        "Hello world"
    }
}

/*
This represents a lifecycle aware object, similar to an Activity,
that holds its own Main coroutine scope. Note that this scope is thus executed on the main thread.
 */
private class Lifecycle {
    private val mainScope = MainScope()

    fun launchCoroutines() {
        repeat(10) {
            mainScope.launch {
                printDelayed(id = it)
            }
        }
    }

    /*
    When the lifecycle has ended the scope is canceled and all it's child coroutines
    are also canceled thanks to structured concurrency
     */
    fun destroy() {
        mainScope.cancel()
    }
}