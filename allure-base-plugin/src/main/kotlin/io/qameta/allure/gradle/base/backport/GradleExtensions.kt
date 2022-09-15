package io.qameta.allure.gradle.util

import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named

fun AttributeContainer.categoryLibrary(objects: ObjectFactory) {
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
}

fun AttributeContainer.categoryDocumentation(objects: ObjectFactory) {
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
}

fun AttributeContainer.libraryElementsJar(objects: ObjectFactory) {
    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
}
