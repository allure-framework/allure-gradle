package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.adapter.config.AllureJavaAdapter
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

/** Validate the jars on each actual test runtime, allowing separate runtimes to migrate independently. */
internal class ValidateAllureJavaRuntime : Action<Task> {
    override fun execute(task: Task) {
        val (classpath, launcher) = when (task) {
            is Test -> task.classpath to task.javaLauncher
            is JavaExec -> task.classpath to task.javaLauncher
            else -> return
        }
        // Gradle's resolved Maven artifacts retain their artifact-version[-classifier].jar names.
        val modules = AllureJavaAdapter.values().map { "allure-${it.adapterName}" } + listOf(
            "allure-java-commons", "allure-model", "allure-jupiter", "allure-junit4-aspect",
            "allure-scalatest_2.12", "allure-scalatest_2.13", "allure-scalatest_3"
        )
        val pattern = Regex("(${modules.joinToString("|") { Regex.escape(it) }})-([23])\\..*\\.jar")
        val sdkJars = classpath.files.mapNotNull { file -> pattern.matchEntire(file.name) }
        if (sdkJars.none { it.groupValues[2] == "3" }) return

        if (sdkJars.any { it.groupValues[2] == "2" }) {
            throw GradleException(
                "${task.path} mixes Allure Java 2.x and 3.x on one test runtime: " +
                    sdkJars.joinToString { it.value } + ". Align the adapters and direct Allure Java dependencies " +
                    "to one SDK major version per test runtime."
            )
        }
        val minimumJava = if (sdkJars.any { it.groupValues[1] == "allure-karate" }) 21 else 17
        val actualJava = launcher.get().metadata.languageVersion.asInt()
        if (actualJava < minimumJava) {
            throw GradleException(
                "${task.path} uses Allure Java 3.x${if (minimumJava == 21) " with Karate 2" else ""}, " +
                    "which requires Java $minimumJava or newer; its test launcher uses Java $actualJava. " +
                    "Configure the test task's javaLauncher/toolchain or select Allure Java 2.x."
            )
        }
    }
}
