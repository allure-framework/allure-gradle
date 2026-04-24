package io.qameta.allure.gradle.allure;

import io.qameta.allure.gradle.rule.GradleRunnerRule;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CategoriesTest {

    @TempDir
    File tempDir;

    static Collection<org.junit.jupiter.params.provider.Arguments> getFrameworks() {
        return List.of(
                arguments("9.4.1", "src/it/categories", new String[]{"allureReport"}),
                arguments("8.14.3", "src/it/categories", new String[]{"allureReport"}),
                arguments("8.11.1", "src/it/categories", new String[]{"allureReport"})
        );
    }

    @ParameterizedTest(name = "{1} [{0}]")
    @MethodSource("getFrameworks")
    void shouldCopyCategoriesInfo(String version, String project, String[] tasks) {
        GradleRunnerRule gradleRunner = new GradleRunnerRule()
                .rootDir(tempDir)
                .version(version)
                .project(project)
                .tasks(tasks)
                .build();

        File resultsDir = new File(gradleRunner.getProjectDir(), "build/allure-results");

        assertThat(resultsDir.listFiles()).as("Allure executor info")
                .filteredOn(file -> file.getName().endsWith("categories.json"))
                .hasSize(1);
    }
}
