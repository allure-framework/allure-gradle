package io.qameta.allure.gradle.plugin.test.extension

import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo
import spock.lang.Specification

import java.lang.annotation.Annotation

@CompileStatic
abstract class AbstractFieldInitInterceptor<T extends Annotation> extends AbstractMethodInterceptor {

    protected FieldInfo field
    protected T annotation

    protected AbstractFieldInitInterceptor(T annotation, FieldInfo field) {
        this.annotation = annotation
        this.field = field
    }

    abstract void init(T annotation, IMethodInvocation invocation, Specification specification)

    @Override
    void interceptSetupMethod(IMethodInvocation invocation) throws Throwable {
        if (!field.shared) {
            init annotation, invocation, getSpecification(invocation)
        }
    }

    @Override
    void interceptSetupSpecMethod(IMethodInvocation invocation) throws Throwable {
        if (field.shared) {
            init annotation, invocation, getSpecification(invocation)
        }
    }

    void install(SpecInfo spec) {
        def interceptors = field.shared ? spec.setupSpecInterceptors : spec.setupInterceptors
        interceptors.add this
    }

    private static final Specification getSpecification(IMethodInvocation invocation) {
        (invocation.instance ?: invocation.sharedInstance) as Specification
    }

}
