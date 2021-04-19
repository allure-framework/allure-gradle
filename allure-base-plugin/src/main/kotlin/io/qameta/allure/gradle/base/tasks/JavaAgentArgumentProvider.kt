package io.qameta.allure.gradle.base.tasks

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Classpath
import org.gradle.process.CommandLineArgumentProvider

class JavaAgentArgumentProvider(classPath: Configuration) : CommandLineArgumentProvider {
    @get:Classpath
    val agentJar: Configuration = classPath

    override fun asArguments() = listOf("-javaagent:${agentJar.singleFile}")
}
