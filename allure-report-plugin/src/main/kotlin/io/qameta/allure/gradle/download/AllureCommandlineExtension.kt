package io.qameta.allure.gradle.download

import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.util.conv
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class AllureCommandlineExtension @Inject constructor(
    allureExtension: AllureExtension,
    project: Project,
    objects: ObjectFactory
) {
    companion object {
        const val NAME = "commandline"
    }

    /**
     * Configure `groupId` for `allure-commandline` artifact retrieval.
     * Default value is `io.qameta.allure` if [downloadUrlPattern] is not set
     * or 'custom.io.qameta.allure` if [downloadUrlPattern] is set.
     * The use of `custom.io...` group id enables to use Gradle's repository filtering
     * for better security and resolution performance.
     * The property corresponds to `[organization]` pattern in [downloadUrlPattern]
     */
    val group = objects.property<String>()
        .conv(project.provider {
            // TODO: Provider.orElse(Provider) is Gradle 5.6+
            if (downloadUrlPattern.isPresent) {
                "custom.io.qameta.allure"
            } else {
                "io.qameta.allure"
            }
        })

    /**
     * Configure `artifactId` for `allure-commandline` artifact retrieval.
     * Default value is `allure-commandline` for Allure 2.8.0+ and `allure` for earlier versions.
     * It corresponds to `[module]` pattern in [downloadUrlPattern]
     */
    val module = objects.property<String>()
        .conv(allureExtension.version.map {
            val normalized = it.replace(Regex("\\d+")) {
                it.value.padStart(5, '0')
            }
            if (normalized >= "00002.00008.00000") {
                "allure-commandline"
            } else {
                "allure"
            }
        })

    /**
     * Configure `groupId` for `allure-commandline` artifact retrieval.
     * Default value is `zip`
     * It corresponds to `[ext]` pattern in [downloadUrlPattern]
     */
    val extension = objects.property<String>()
        .conv("zip")

    /**
     * By default, allure-commandline is received from Maven Central, so the property is unset.
     * This property allows overriding the url.
     * The following patterns are supported: `[group]`, `[module]`, `[version]`, `[extension]`
     */
    val downloadUrlPattern = objects.property<String>()
}
