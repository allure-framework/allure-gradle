plugins {
    id("org.jetbrains.dokka")
}

// https://github.com/gradle/gradle/pull/16627
inline fun <reified T: Named> AttributeContainer.attribute(attr: Attribute<T>, value: String) =
    attribute(attr, objects.named<T>(value))

val javadocMainElements by configurations.creating {
    isVisible = false
    description = "Javadoc code elements"
    isCanBeResolved = false
    isCanBeConsumed = true

    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, Category.DOCUMENTATION)
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, DocsType.JAVADOC)
        attribute(Usage.USAGE_ATTRIBUTE, Usage.JAVA_RUNTIME)
        attribute(Bundling.BUNDLING_ATTRIBUTE, Bundling.EXTERNAL)
    }
}

val javadocJar by tasks.registering(Jar::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Assembles a jar archive containing javadoc"
    from(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
}

javadocMainElements.outgoing.artifact(javadocJar)

(components["java"] as AdhocComponentWithVariants).addVariantsFromConfiguration(javadocMainElements) {
    mapToOptional()
    mapToMavenScope("runtime")
}
