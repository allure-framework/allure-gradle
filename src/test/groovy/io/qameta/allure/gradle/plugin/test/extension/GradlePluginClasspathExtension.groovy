package io.qameta.allure.gradle.plugin.test.extension

import groovy.transform.InheritConstructors
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.FieldInfo
import spock.lang.Specification

class GradlePluginClasspathExtension extends AbstractAnnotationDrivenExtension<GradlePluginClasspath> {

    @Override
    void visitFieldAnnotation(GradlePluginClasspath annotation, FieldInfo field) {
        new GradlePluginClasspathInterceptor(annotation, field)
                .install(field.parent.getTopSpec())
    }

    @InheritConstructors
    static class GradlePluginClasspathInterceptor extends AbstractFieldInitInterceptor<GradlePluginClasspath> {

        @Override
        void init(GradlePluginClasspath annotation, IMethodInvocation invocation, Specification specification) {
            specification."$field.name" = pluginClasspathResource
                    .readLines()
                    .collect { new File(it) }
            invocation.proceed()
        }

        private URL getPluginClasspathResource() {
            def resource = getClass().classLoader.getResource("plugin-classpath.txt")
            if (resource == null) {
                throw new IllegalStateException(
                        "Did not find plugin classpath resource, run `testClasses` build task.")
            }
            resource
        }

    }
}

