package io.qameta.allure.gradle.base.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.process.CommandLineArgumentProvider

class JavaAgentArgumentProvider(classPath: FileCollection) : CommandLineArgumentProvider {
    @get:Classpath
    val agentJar: FileCollection = classPath

    override fun asArguments() = listOf("-javaagent:${agentJar.singleFile}")
}
