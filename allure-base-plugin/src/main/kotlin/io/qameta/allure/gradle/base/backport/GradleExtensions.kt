package io.qameta.allure.gradle.util

import org.gradle.api.DomainObjectSet
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.named
import org.gradle.util.GradleVersion
import org.gradle.util.WrapUtil

val gradleGe51 = GradleVersion.current() >= GradleVersion.version("5.1")
val gradleGe53 = GradleVersion.current() >= GradleVersion.version("5.3")
val gradleGe55 = GradleVersion.current() >= GradleVersion.version("5.5")
val gradleGe56 = GradleVersion.current() >= GradleVersion.version("5.6")
val gradleGe65 = GradleVersion.current() >= GradleVersion.version("6.5")

fun <T> Property<T>.conv(v: T) = if (gradleGe51) convention(v) else apply { set(v) }
fun <T> Property<T>.conv(v: Provider<out T>) = if (gradleGe51) convention(v) else apply { set(v) }

fun <T> ListProperty<T>.conv(v: Iterable<T>) = if (gradleGe51) convention(v) else apply { set(v) }
fun <T> ListProperty<T>.conv(v: Provider<out Iterable<T>>) = if (gradleGe51) convention(v) else apply { set(v) }

fun <K, V> MapProperty<K, V>.conv(v: Map<K, V>) = if (gradleGe51) convention(v) else apply { set(v) }
fun <K, V> MapProperty<K, V>.conv(v: Provider<out Map<K, V>>) = if (gradleGe51) convention(v) else apply { set(v) }

fun <T> Property<T>.forUseAtConfigurationTimeBackport(): Property<T> = apply {
    if (gradleGe65) {
        // forUseAtConfigurationTime is Gradle 6.5+ feature
        forUseAtConfigurationTime()
    }
}

inline fun <reified T> ObjectFactory.domainObjectSetBackport(): DomainObjectSet<T> =
    if (gradleGe55) domainObjectSet(T::class.java) else WrapUtil.toDomainObjectSet(T::class.java)

fun AttributeContainer.categoryLibrary(objects: ObjectFactory) {
    if (gradleGe53) {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

fun AttributeContainer.categoryDocumentation(objects: ObjectFactory) {
    if (gradleGe53) {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
    }
}

fun AttributeContainer.libraryElementsJar(objects: ObjectFactory) {
    if (gradleGe56) {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}
