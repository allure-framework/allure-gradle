package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleTestVersion
import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JavadocDescriptionsAutoconfigureTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `javadoc descriptions processor should be added by default`() {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(GradleTestVersion.current())
            .project("src/it/adapter-javadoc-descriptions")
            .tasks("writeJavadocDescriptionArtifacts")
            .build()

        val processors = gradleRunner.projectDir.resolve("build/testAnnotationProcessors.txt").readText()
        val descriptions = gradleRunner.projectDir.resolve("build/javadocDescriptions.txt").readText()

        assertThat(processors)
            .contains("io.qameta.allure:allure-descriptions-javadoc:${AllureAdapterExtension.DEFAULT_ALLURE_JAVA_VERSION}")
        assertThat(descriptions)
            .contains("A description from Javadoc.")
    }

    @Test
    fun `javadoc descriptions processor should have an opt-out`() {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(GradleTestVersion.current())
            .project("src/it/adapter-javadoc-descriptions")
            .tasks("writeJavadocDescriptionArtifacts", "-PdisableJavadocDescriptions=true")
            .build()

        val processors = gradleRunner.projectDir.resolve("build/testAnnotationProcessors.txt").readText()
        val descriptions = gradleRunner.projectDir.resolve("build/javadocDescriptions.txt").readText()

        assertThat(processors)
            .doesNotContain("io.qameta.allure:allure-descriptions-javadoc:")
        assertThat(descriptions)
            .isBlank()
    }
}
