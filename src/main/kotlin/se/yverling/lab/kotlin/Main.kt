package se.yverling.lab.kotlin

import se.yverling.lab.kotlin.coroutines.CoroutinesBench
import se.yverling.lab.kotlin.flow.FlowBench
import se.yverling.lab.kotlin.kotlin.KotlinBench
import se.yverling.lab.kotlin.kotlin.KotlinBench.returnInAssignment

fun main() {
    KotlinBench.all()
    CoroutinesBench.all()
    FlowBench.all()
}
