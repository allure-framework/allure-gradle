package io.qameta.allure.gradle.base.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.process.CommandLineArgumentProvider

class JavaAgentArgumentProvider(
    @get:Input
    val enabled: Provider<Boolean>,
    classPath: FileCollection
) : CommandLineArgumentProvider {
    @get:Classpath
    val agentJar: FileCollection = classPath

    override fun asArguments() = if (enabled.get()) {
        listOf("-javaagent:${agentJar.singleFile}")
    } else {
        emptyList()
    }
}
