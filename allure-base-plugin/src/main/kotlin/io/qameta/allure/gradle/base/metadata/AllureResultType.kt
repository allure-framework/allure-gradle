package io.qameta.allure.gradle.base.metadata

import org.gradle.api.attributes.Attribute

enum class AllureResultType {
    RAW,
    CATEGORIES,

    @Deprecated(
        message = "This category would be removed once Allure would support multiple categories.json files",
        level = DeprecationLevel.WARNING
    )
    COPY_CATEGORIES
    ;

    companion object {
        val ATTRIBUTE = Attribute.of("io.qameta.allure", AllureResultType::class.java)
    }
}
