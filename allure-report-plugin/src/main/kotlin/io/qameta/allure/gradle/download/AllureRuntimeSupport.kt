package io.qameta.allure.gradle.download

import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.base.AllureRuntimeFamily
import io.qameta.allure.gradle.base.allureRuntimeFamily
import org.gradle.api.plugins.ExtensionAware

internal const val DEFAULT_NODE_VERSION = "22.22.0"

internal data class NodeDistribution(
    val classifier: String,
    val extension: String,
)

internal fun allureRuntimeFamily(version: String) = version.allureRuntimeFamily()

internal fun detectNodeDistribution(
    osName: String = System.getProperty("os.name"),
    archName: String = System.getProperty("os.arch"),
): NodeDistribution {
    val os = osName.lowercase()
    val arch = archName.lowercase()

    val normalizedArch = when (arch) {
        "aarch64", "arm64" -> "arm64"
        "amd64", "x86_64" -> "x64"
        else -> throw IllegalArgumentException(
            "Unsupported architecture for Allure 3 runtime: $archName. " +
                "Supported architectures: x64, arm64."
        )
    }

    return when {
        os.contains("mac") || os.contains("darwin") ->
            NodeDistribution("darwin-$normalizedArch", "tar.gz")
        os.contains("linux") ->
            NodeDistribution("linux-$normalizedArch", "tar.gz")
        os.contains("windows") ->
            NodeDistribution("win-$normalizedArch", "zip")
        else -> throw IllegalArgumentException(
            "Unsupported operating system for Allure 3 runtime: $osName. " +
                "Supported operating systems: macOS, Linux, Windows."
        )
    }
}

internal fun AllureExtension.commandlineExtension(): AllureCommandlineExtension =
    (this as ExtensionAware).extensions.getByType(AllureCommandlineExtension::class.java)

internal fun AllureExtension.hasAllure2CommandlineCustomization(): Boolean {
    if (version.get().allureRuntimeFamily() == AllureRuntimeFamily.ALLURE_2) {
        return false
    }
    val commandline = commandlineExtension()
    val expectedModule = "allure-commandline"
    return commandline.downloadUrlPattern.orNull != null ||
        commandline.group.orNull != "io.qameta.allure" ||
        commandline.module.orNull != expectedModule ||
        commandline.extension.orNull != "zip"
}
