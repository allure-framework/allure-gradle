package io.qameta.allure.gradle.base

enum class AllureRuntimeFamily {
    ALLURE_2,
    ALLURE_3,
}

fun String.allureRuntimeFamily(): AllureRuntimeFamily {
    val major = substringBefore('.').toIntOrNull()
        ?: throw IllegalArgumentException("Unsupported Allure version: $this")
    return if (major >= 3) {
        AllureRuntimeFamily.ALLURE_3
    } else {
        AllureRuntimeFamily.ALLURE_2
    }
}
