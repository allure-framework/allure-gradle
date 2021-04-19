package io.qameta.allure.gradle.base.tasks

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.process.CommandLineArgumentProvider

/**
 * Enables lazy computation of the command line arguments.
 * Note: do not use this class if the value of the option depends on the file contents.
 */
class ConditionalArgumentProvider(
    @Input
    val args: Provider<Iterable<String>>
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> = args.get()
}
