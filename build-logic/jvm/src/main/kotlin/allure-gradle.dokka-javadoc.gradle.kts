import org.gradle.api.Task
plugins {
    // Apply only the Javadoc plugin (includes Dokka base)
    id("org.jetbrains.dokka-javadoc")
}

// https://github.com/gradle/gradle/pull/16627
inline fun <reified T: Named> AttributeContainer.attribute(attr: Attribute<T>, value: String) =
    attribute(attr, objects.named<T>(value))

pluginManager.withPlugin("org.jetbrains.dokka-javadoc") {
    val javadocMainElements: Configuration by configurations.creating {
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

    // Configure Dokka v2 Javadoc generation task lazily without compile-time Dokka types
    val dokkaJavadoc = tasks.named("dokkaGeneratePublicationJavadoc")

    val javadocJar by tasks.registering(Jar::class) {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Assembles a jar archive containing javadoc"
        // Use task output directory provider via reflective access to avoid   Dokka API dependency
        val outputDirProvider = dokkaJavadoc.flatMap { t: Task ->
            @Suppress("UNCHECKED_CAST")
            val dirProvider = t::class.java.getMethod("getOutputDirectory").invoke(t) as Provider<Directory>
            dirProvider.map { it.asFileTree }
        }
        from(outputDirProvider)
        archiveClassifier.set("javadoc")
    }

    javadocMainElements.outgoing.artifact(javadocJar)

    tasks.named("assemble").configure { dependsOn(javadocJar) }

    (components["java"] as AdhocComponentWithVariants).addVariantsFromConfiguration(javadocMainElements) {
        mapToOptional()
        mapToMavenScope("runtime")
    }
}

