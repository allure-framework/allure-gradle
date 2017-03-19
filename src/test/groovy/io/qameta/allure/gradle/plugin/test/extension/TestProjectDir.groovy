package io.qameta.allure.gradle.plugin.test.extension

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@ExtensionAnnotation(TestProjectDirExtension)
@interface TestProjectDir {
    String dir() default 'testkit'
    boolean clean() default true
    boolean copy() default true
}
