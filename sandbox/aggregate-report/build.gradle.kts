plugins {
    id("io.qameta.allure-aggregate-report")
}

dependencies {
    "allureAggregateReport"(project(":dsl-kotlin"))
}
