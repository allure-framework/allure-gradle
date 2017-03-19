package io.qameta.allure.gradle.plugin.test.extension

import groovy.transform.InheritConstructors
import org.apache.commons.io.FileUtils
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.FieldInfo
import spock.lang.Specification

class TestProjectDirExtension extends AbstractAnnotationDrivenExtension<TestProjectDir> {

    @Override
    void visitFieldAnnotation(TestProjectDir annotation, FieldInfo field) {
        new TestProjectDirInterceptor(annotation, field)
                .install(field.parent.getTopSpec())
    }

    @InheritConstructors
    static class TestProjectDirInterceptor extends AbstractFieldInitInterceptor<TestProjectDir> {

        @Override
        void init(TestProjectDir annotation, IMethodInvocation invocation, Specification specification) {
            def to = new File("build/gradle-testkit", annotation.dir())
            def from = new File("src/data", annotation.dir())
            assert from.isDirectory()
            if (to.exists() && annotation.clean()) {
                FileUtils.cleanDirectory(to)
            }
            if (annotation.copy()) {
                FileUtils.copyDirectory(from, to)
            }
            specification."$field.name" = to
            invocation.proceed()
        }

    }

}
