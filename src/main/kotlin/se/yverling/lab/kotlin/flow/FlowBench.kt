package se.yverling.lab.kotlin.flow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.reflect.KVisibility

@OptIn(ExperimentalCoroutinesApi::class)
object FlowBench {
    fun all() {
        FlowBench::class.members.forEach {
            if (it.name !in listOf("all", "hashCode", "toString", "equals") && it.visibility == KVisibility.PUBLIC) {
                println("--- ${it.name} ---")
                it.call(FlowBench)
                println()
            }
        }
    }

    fun `Stream values using a flow`() = runBlocking {
        launch {
            repeat(5) {
                println("Main: I'm not blocked")
                delay(500)
            }
        }

        val list = flowOfNumbers()
        println("Main: flowOfNumbers() has been called")

        /*
          Values in a flow are collected using collect().

          Flows are cold streams and the code in flowOfNumbers() is only invoked when
          collect() is called.
         */
        list.collect { println(it) }
    }

    // Note that this is not a suspendable method
    private fun flowOfNumbers(): Flow<Int> = flow {
        println("Flow started")
        repeat(10) {
            delay(500)
            // Values in a flow are emitted using emit()
            emit(it)
        }
    }

    fun `Canceling a flow (using withTimeOut())`() = runBlocking {
        try {
            // This will cancel the flow after 5 s
            withTimeout(5000) {
                // Note that there are several other flow builder such as .asFlow() and flowOf()
                (1..3).asFlow().collect {
                    printDelayed()
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("Got timeout")
        }
    }

    // The "suspend" modifier is used when a function contains a suspending call, such as delay
    private suspend fun printDelayed(delayInMillis: Long = 1000, id: Int = 1) {
        delay(delayInMillis)
        println("Coroutine #$id has delayed $delayInMillis ms")
    }

    fun `Transforming a flow with operators`() = runBlocking {
        (1..3).asFlow()
            .map { it * 2 }
            .collect {
                println(it)
            }
    }

    fun `Size limiting a flow`() = runBlocking {
        (1..3).asFlow()
            // Only take the first two emitted values
            .take(2)
            .collect {
                println(it)
            }
    }

    /*
        Terminal operators on flows are suspending functions that start a collection of the flow.
        The collect() operator is the most basic one, but there are other terminal operators as well.
     */
    fun `Terminal operators`() = runBlocking {
        val sum = (1..3).asFlow()
            .map { it * 2 }
            // This will sum each emitted value with the previous value (2, 6, 12)
            .reduce { accumulator, value -> accumulator + value }

        println(sum)
    }

    fun `Emit values on background thread`() = runBlocking {
        flow {
            repeat(3) {
                delay(1000)
                println("Emitting on ${Thread.currentThread().name}")
                emit(it)
            }

            /*
                This will run the flow the Default dispatcher (instead of the default Main).
                Note that the use of withContext() is not allowed in flow, but only flowOn().
             */
        }.flowOn(Dispatchers.Default)
            .collect {
                println("Collecting on ${Thread.currentThread().name}")
                println(it)
            }
    }

    fun `Combining flows`() = runBlocking {
        val numberFlow = flowOf(1, 2, 3, 4)
        val charFlow = flowOf("a", "b", "c")

        // zip() always concatenate a pair of events, if only one flow emits a new event, it will dismiss it
        numberFlow.zip(charFlow) { number, char ->
            "$number : $char"
        }.collect {
            println(it)
        }

        /*
        In contrast to zip(), if only one flow emits a new event combine() will use
         the last emitted event from the other flow to make a pair
         */
        combine(numberFlow, charFlow) { number, char ->
            "$number : $char"
        }.collect {
            println(it)
        }
    }

    /*
       A common scenario for flatMap*() could be that you want to make a network request
        for each value emitted from another network request.
     */
    fun `Chaining and flattening flows`() = runBlocking {
        val firstFlow = flowOf(1, 2, 3)
        val secondFlow = flowOf("a", "b", "c", "d").onEach { delay(300) }

        println()
        println("flatMapConcat")

        // This will emit the all events in secondFlow for every event in firstFlow
        firstFlow.flatMapConcat {
            secondFlow
        }.collect {
            println(it)
        }

        println()
        println("flatMapMerge")

        // This will emit one event in secondFlow for one event in firstFlow
        firstFlow.flatMapMerge {
            secondFlow
        }.collect {
            println(it)
        }

        println()
        println("flatMapLatest")

        /* This will emit all event in secondFlow for the latest event firstFlow.
        If first flow emits another value the secondFlow will cancel and and start with the
         latest value emitted from firstFlow */
        firstFlow.flatMapLatest {
            secondFlow
        }.collect {
            println(it)
        }
    }

    fun `Exceptions in a flow`() = runBlocking {
        val flow = flowOf(1, 2, 3)
            .onEach {
                // This will throw an IllegalStateException if the criteria is not met
                check(it > 2)
            }

        /*
             This is convenience method for exception handling.
             We could also wrap the whole call to the flow in a try/catch to catch the exception.
         */
        flow.catch {
            println("Caught exception $it")
        }.collect {
            println("Collect")
        }

        println("---")

        try {
            flow.collect {
                println("Collect")
            }
        } catch (e: Exception) {
            println("Caught exception $e")
        } finally {
            println("Finally")
        }
    }

    fun `Run flow, but do not collect`() = runBlocking {
        flow {
            repeat(3) {
                delay(1000)
                emit(it)
                println("Value emitted")
            }

            // This will launch the flow, but not collect it. It could be useful for event handlers.
        }.launchIn(this)
        println("Flow is up and running. Main can do other things.")
    }

    /*
        This is an example on how you could pass a parameter to a flow builder to "update" the flow
     */

    fun `Updating flow`() = runBlocking {
        var flow = flowWithParam(1)

        flow.collect {
            println(it)
        }

        flow = flowWithParam(2)

        flow.collect {
            println(it)
        }
    }

    private fun flowWithParam(modifier: Int): Flow<Int> = flow {
        println("Flow started")
        repeat(10) {
            emit(it * modifier)
        }
    }

    fun `Debouncing a flow`() = runBlocking {
        val flow = flow {
            emit(1)
            delay(90)
            emit(2)
            delay(90)
            emit(3)
            delay(1010)
            emit(4)
            delay(1010)
            emit(5)
        }.debounce(1000)

        flow.collect {
            println(it)
        }
    }

    fun `Collect only a single value and get out of the coroutine`() {
        var number: Int? = null

        runBlocking {
            val flow = flow {
                emit(1)
            }

            /*
            Note that single(), in contrast to first(), will wait
             until the coroutine is closed before it terminates
             */
            number = flow.first()
        }

        println("Got number: $number")
    }


    fun `Collect from a flow until a certain condition has been met using transformWhile()`() =
        runBlocking {
            var counter = 0
            flow {
                while (true) {
                    emit(counter++)
                }
            }
                .transformWhile {
                    emit(it)
                    it < 10
                }
                .collect {
                    println(it)
                }
        }

    /*
     * SharedFlows are hot flows. These are the differences between cold and hot flows:
     *
     * Cold flow
     * ---
     * - Only emits data when there's a collector
     * - Stores no data (stateless)
     * - Can't have multiple collectors in parallel
     *
     * Hot flow
     * ---
     * - Emits data even if no one is collection events
     * - Stores data (stateful)
     * - Can have multiple collectors in parallel
     */
    fun `Using hot flow with SharedFlow`() = runBlocking {
        // This will replay the last two events to new subscribers
        val _sharedFlow = MutableSharedFlow<Int>(replay = 2)
        val sharedFlow = _sharedFlow.asSharedFlow()

        launch {
            _sharedFlow.emit(1) // This event will be dropped
            _sharedFlow.emit(2)
            _sharedFlow.emit(3)
        }

        val collectorJob = launch {
            sharedFlow.collect {
                println("Collector got event: $it")
            }
        }

        collectorJob.cancel()
    }

    /*
     * StateFlow is a simplification of SharedFlow which will drop the oldest events and only emit the newest.
     * This is thus a good way to model state changes, for example in an UI though a ViewModel.
     */
    fun `Using hot flow with StateFlow`() = runBlocking {
        val stateFlow = MutableStateFlow(0)

        stateFlow.value = 1

        // Both collector jobs below will miss this event
        stateFlow.value = 2

        val collectorJob1 = launch {
            stateFlow.collect {
                println("Collector 1 got event: $it")
            }
        }

        val collectorJob2 = launch {
            stateFlow.collect {
                println("Collector 2 got event: $it")
            }
        }

        // Collector job 2 will miss this event
        stateFlow.value = 3

        collectorJob1.cancel()
        collectorJob2.cancel()
    }

    fun `WIP StateFlow on different threads`() = runBlocking {
        val stateFlow = MutableStateFlow(0)

        val collectorJob1 = launch {
            stateFlow.collect {
                println("Collector got event: $it on ${Thread.currentThread().name}")
            }
        }

        launch {
            launch(Dispatchers.Default) {
                println("Emitting 1 on ${Thread.currentThread().name}")
                stateFlow.update { 1 }
            }

            delay(100)

            launch(Dispatchers.Default) {
                println("Emitting 2 on ${Thread.currentThread().name}")
                stateFlow.update { 2 }
            }
        }

        collectorJob1.cancel()
    }


    fun `Converting a callback interface to a flow`() = runBlocking {
        callbackFlow {
            val callback = object : Listener {
                override fun onSuccess(data: Int) {
                    trySend(data)
                }
            }
            Producer.register(callback)

            awaitClose {
                // Unregister listeners when flow is closed
                Producer.unregisterAll()
            }
        }
            .take(3)
            .collect {
                println(it)
            }
    }

    private interface Listener {
        fun onSuccess(data: Int)
    }

    private object Producer {
        var listen = true

        fun register(listener: Listener) {
            var i = 0
            // Prints up to 10 numbers
            while (listen && i < 10) {
                listener.onSuccess(i++)
            }
        }

        fun unregisterAll() {
            listen = false
        }
    }
}
