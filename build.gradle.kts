@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.util.Locale

plugins {
    application
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.powerAssert)
    alias(libs.plugins.ktlint)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutableCollections)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.coroutines)
    testImplementation(libs.test.kotest.assertions)
}

application {
    mainClass.set("se.yverling.lab.kotlin.MainKt")
}

ktlint {
    ignoreFailures = true
}
