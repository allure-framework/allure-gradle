package io.qameta.allure.gradle.allure;

import groovy.json.JsonSlurper;
import io.qameta.allure.Allure;
import io.qameta.allure.gradle.rule.GradleRunnerRule;
import io.qameta.allure.gradle.rule.GradleTestVersion;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AllureJavaCompatibilityTest {
    @TempDir
    File tempDir;

    @ParameterizedTest(name = "{0} with SDK {1}")
    @CsvSource({
        "junit4-autoconfigure,2.35.5,step", "junit4-autoconfigure,3.0.0,step",
        "jupiter-sdk-compatibility,2.35.5,step", "jupiter-sdk-compatibility,3.0.0,step",
        "testng-autoconfigure,2.35.5,step", "testng-autoconfigure,3.0.0,step",
        "spock,2.35.5,step", "spock,3.0.0,step",
        "cucumber7-jvm,2.35.5,Given step", "cucumber7-jvm,3.0.0,Given step"
    })
    void emitsResultStepAndAttachment(String fixture, String sdk, String step) throws Exception {
        GradleRunnerRule runner = prepare(fixture, sdk);
        assertThat(runner.run("test").task(":test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        verifyEvidence(runner, step);
    }

    @ParameterizedTest(name = "{0} listeners disabled with SDK {1} via {2}")
    @CsvSource({
        "jupiter-sdk-compatibility,2.35.5,jupiter", "jupiter-sdk-compatibility,3.0.0,jupiter",
        "jupiter-sdk-compatibility,2.35.5,junit5", "jupiter-sdk-compatibility,3.0.0,junit5",
        "jupiter,2.13.5,jupiter",
        "testng-autoconfigure,2.35.5,testng", "testng-autoconfigure,3.0.0,testng"
    })
    void listenerOptOutIncludesTransitiveListeners(String fixture, String sdk, String adapter) throws Exception {
        GradleRunnerRule runner = prepare(fixture, sdk);
        append(runner, "allure.adapter.frameworks." + adapter + ".autoconfigureListeners = false");
        assertThat(runner.run("test").task(":test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        Allure.step("Verify that disabled listeners emitted no Allure test results", () ->
            assertThat(results(runner)).isEmpty());
    }

    @Test
    void explicitPlatformListenerSurvivesJupiterOptOut() throws Exception {
        GradleRunnerRule runner = prepare("jupiter-sdk-compatibility", "3.0.0");
        append(runner, """
            allure.adapter.frameworks.jupiter.autoconfigureListeners = false
            allure.adapter.frameworks.junitPlatform.autoconfigureListeners = true
            """);
        runner.run("test");
        verifyEvidence(runner, "step");
    }

    @Test
    void minimumSupportedTestNgExecutesWithSdk3() throws Exception {
        GradleRunnerRule runner = prepare("testng-autoconfigure", "3.0.0");
        Path build = runner.getProjectDir().toPath().resolve("build.gradle");
        Files.writeString(build, Files.readString(build).replace("7.12.0", "7.10.0"));
        runner.run("test");
        verifyEvidence(runner, "step");
    }

    @ParameterizedTest(name = "SDK {0} reuses configuration cache")
    @ValueSource(strings = {"2.35.5", "3.0.0"})
    void executesAgainFromConfigurationCache(String sdk) throws Exception {
        GradleRunnerRule runner = prepare("jupiter-sdk-compatibility", sdk);
        BuildResult first = runner.run("test", "--configuration-cache", "--rerun-tasks");
        assertThat(first.getOutput()).contains("Configuration cache entry stored.");
        verifyEvidence(runner, "step");
        // Remove only this fixture's results so the second execution must produce fresh evidence.
        try (var files = Files.list(resultsDir(runner))) {
            for (Path path : files.toList()) Files.delete(path);
        }
        BuildResult second = runner.run("test", "--configuration-cache", "--rerun-tasks");
        assertThat(second.getOutput()).containsPattern("(Reusing configuration cache\\.|Configuration cache entry reused\\.)");
        assertThat(second.task(":test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        verifyEvidence(runner, "step");
    }

    @ParameterizedTest(name = "SDK 3 rejects legacy {0}:{1} on the same test runtime")
    @CsvSource({"allure-testng,2.35.5", "allure-junit5,2.34.0"})
    void mixedSdkMajorsFailBeforeTestsStart(String module, String version) throws Exception {
        GradleRunnerRule runner = prepare("jupiter-sdk-compatibility", "3.0.0");
        append(runner, "dependencies { testImplementation 'io.qameta.allure:" + module + ":" + version + "' }");
        BuildResult result = failure(runner, "test");
        assertThat(result.getOutput()).contains("mixes Allure Java 2.x and 3.x on one test runtime",
            module + "-" + version + ".jar", "one SDK major version per test runtime");
        assertThat(results(runner)).isEmpty();
    }

    @Test
    void separateTestRuntimesCanUseDifferentSdkMajors() throws Exception {
        GradleRunnerRule runner = prepare("jupiter-sdk-compatibility", "3.0.0");
        append(runner, """
            allure.adapter.autoconfigure = false
            allure.adapter.aspectjWeaver = true
            configurations { legacyRuntime }
            dependencies {
                testImplementation 'io.qameta.allure:allure-jupiter:3.0.0'
                legacyRuntime 'io.qameta.allure:allure-jupiter:2.35.5'
                legacyRuntime 'org.junit.jupiter:junit-jupiter:6.1.3'
                legacyRuntime 'org.junit.platform:junit-platform-launcher:6.1.3'
            }
            tasks.register('legacyTest', Test) {
                testClassesDirs = sourceSets.test.output.classesDirs
                classpath = sourceSets.test.output + configurations.legacyRuntime
                useJUnitPlatform()
                mustRunAfter test
            }
            tasks.withType(Test).configureEach {
                systemProperty 'junit.jupiter.extensions.autodetection.enabled', 'true'
            }
            """);
        BuildResult result = runner.run("test", "legacyTest");
        assertThat(result.task(":test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.task(":legacyTest").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(results(runner)).hasSize(2);
        for (Path file : results(runner)) {
            Allure.attachment("Runtime result", "application/json", Files.readString(file));
            assertThat(parse(file)).containsEntry("status", "passed");
        }
    }

    @Test
    void validatesTheTestLauncherInsteadOfTheGradleJvm() throws Exception {
        GradleRunnerRule runner = prepare("jupiter-sdk-compatibility", "3.0.0");
        append(runner, """
            test.javaLauncher = javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(11)
            }
            """);
        assertThat(failure(runner, "test").getOutput()).contains(
            "requires Java 17 or newer; its test launcher uses Java 11", "javaLauncher/toolchain");
    }

    @Test
    @SuppressWarnings("unchecked")
    void karate2RunsOnJava21AndEmitsEvidence() throws Exception {
        GradleRunnerRule runner = prepare("karate2-sdk-compatibility", "3.0.0");
        assertThat(runner.run("test").task(":test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        List<Path> results = results(runner);
        assertThat(results).hasSize(1);
        Allure.attachment("Karate result", "application/json", Files.readString(results.get(0)));
        Map<String, Object> result = parse(results.get(0));
        assertThat(result).containsEntry("status", "passed");
        assertThat((List<Map<String, Object>>) result.get("steps"))
            .anySatisfy(step -> assertThat(step).containsEntry("name", "match message == 'compatibility-proof'")
                .containsEntry("status", "passed"));
        try (var files = Files.list(resultsDir(runner))) {
            List<Path> attachments = files.filter(p -> p.getFileName().toString().contains("-attachment")).toList();
            assertThat(attachments).anySatisfy(path -> assertThat(path).hasContent("compatibility-proof"));
        }
    }

    @Test
    void karate2RequiresAJava21TestLauncher() throws Exception {
        GradleRunnerRule runner = prepare("karate2-sdk-compatibility", "3.0.0");
        append(runner, "test.javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }");
        assertThat(failure(runner, "test").getOutput()).contains(
            "Allure Java 3.x with Karate 2", "requires Java 21 or newer; its test launcher uses Java 17");
    }

    private GradleRunnerRule prepare(String fixture, String sdk) throws IOException {
        GradleRunnerRule runner = new GradleRunnerRule().rootDir(tempDir)
            .version(GradleTestVersion.current()).project("src/it/" + fixture).prepare();
        append(runner, "allure.adapter.allureJavaVersion = '" + sdk + "'\n"
            + "allure.adapter.frameworks.configureEach { adapter -> adapter.adapterVersion.set('" + sdk + "') }");
        return runner;
    }

    private void append(GradleRunnerRule runner, String script) throws IOException {
        Files.writeString(runner.getProjectDir().toPath().resolve("build.gradle"), "\n" + script + "\n", StandardOpenOption.APPEND);
    }

    private BuildResult failure(GradleRunnerRule runner, String... tasks) {
        return GradleRunnerRule.runBuild(runner.getProjectDir(), GradleTestVersion.current(), List.of(tasks),
            () -> runner.newRunner(tasks).buildAndFail());
    }

    private Path resultsDir(GradleRunnerRule runner) {
        return runner.getProjectDir().toPath().resolve("build/allure-results");
    }

    private List<Path> results(GradleRunnerRule runner) throws IOException {
        Path dir = resultsDir(runner);
        if (!Files.exists(dir)) return List.of();
        try (var files = Files.list(dir)) {
            return files.filter(p -> p.toString().endsWith("-result.json")).toList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(Path result) {
        return (Map<String, Object>) new JsonSlurper().parse(result.toFile());
    }

    @SuppressWarnings("unchecked")
    private Stream<Map<String, Object>> steps(Map<String, Object> result) {
        List<Map<String, Object>> children = (List<Map<String, Object>>) result.getOrDefault("steps", List.of());
        return children.stream().flatMap(step -> Stream.concat(Stream.of(step), steps(step)));
    }

    @SuppressWarnings("unchecked")
    private void verifyEvidence(GradleRunnerRule runner, String stepName) throws Exception {
        List<Path> results = results(runner);
        for (Path result : results) {
            Allure.attachment("Generated result", "application/json", Files.readString(result));
        }
        Allure.step("Verify one passed result with its recorded step and attachment payload", () -> {
            assertThat(results).hasSize(1);
            Map<String, Object> result = parse(results.get(0));
            assertThat(result).containsEntry("status", "passed");
            assertThat(steps(result)).anySatisfy(step ->
                assertThat(step).containsEntry("name", stepName).containsEntry("status", "passed"));
            try (var files = Files.list(resultsDir(runner))) {
                List<Path> attachments = files.filter(p -> p.getFileName().toString().contains("-attachment")).toList();
                assertThat(attachments).hasSize(1);
                String payload = Files.readString(attachments.get(0));
                Allure.attachment("Generated attachment content", "text/plain", payload);
                assertThat(payload).isEqualTo("<p>HELLO</p>");
                assertThat(Files.readString(results.get(0))).contains(attachments.get(0).getFileName().toString());
            }
        });
    }
}
