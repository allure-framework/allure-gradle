package io.qameta.allure.gradle.rule;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.JavaVersion;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.junit.Assume;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;


/**
 * eroshenkoam
 * 20.06.17
 */
public class GradleRunnerRule extends ExternalResource {

    private Supplier<String> projectSupplier;

    private Supplier<String[]> tasksSupplier;

    private Supplier<String> versionSupplier;

    private BuildResult buildResult;

    private File projectDir;

    public BuildResult getBuildResult() {
        return this.buildResult;
    }

    public File getProjectDir() {
        return this.projectDir;
    }

    public GradleRunnerRule version(String version) {
        return version(() -> version);
    }

    public GradleRunnerRule version(Supplier<String> version) {
        this.versionSupplier = version;
        return this;
    }

    public GradleRunnerRule project(Supplier<String> supplier) {
        this.projectSupplier = supplier;
        return this;
    }

    public GradleRunnerRule project(String project) {
        return project(() -> project);
    }

    public GradleRunnerRule tasks(Supplier<String[]> supplier) {
        this.tasksSupplier = supplier;
        return this;
    }

    public GradleRunnerRule tasks(String... tasks) {
        return tasks(() -> tasks);
    }

    static class JavaGradle {
        final JavaVersion javaVersion;
        final GradleVersion gradleVersion;

        JavaGradle(JavaVersion javaVersion, String gradleVersion) {
            this.javaVersion = javaVersion;
            this.gradleVersion = GradleVersion.version(gradleVersion);
        }
    }

    protected void before() throws Throwable {
        projectDir = copyProject(projectSupplier.get());
        new File(projectDir, "settings.gradle").createNewFile();
        String gradleVersion = versionSupplier.get();
        GradleVersion testGradle = GradleVersion.version(gradleVersion);

        Optional<JavaGradle> gradleRequirement = Stream.of(
                new JavaGradle(JavaVersion.VERSION_16, "7.0"),
                new JavaGradle(JavaVersion.VERSION_15, "6.7"),
                new JavaGradle(JavaVersion.VERSION_14, "6.3"),
                new JavaGradle(JavaVersion.VERSION_11, "5.0"),
                new JavaGradle(JavaVersion.VERSION_1_8, "2.0"))
                .filter(v -> v.javaVersion.compareTo(JavaVersion.current()) <= 0)
                .findFirst();
        if (!gradleRequirement.isPresent()) {
            throw new IllegalStateException("Runnign with Java " + JavaVersion.current().getMajorVersion() + " is not supported yet");
        }

        checkGradleCompatibility(gradleRequirement.get(), testGradle);

        buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(tasksSupplier.get())
                .withGradleVersion(gradleVersion)
                .withTestKitDir(new File(projectDir.getParentFile().getAbsolutePath(), ".gradle"))
                .withPluginClasspath(readPluginClasspath())
                .forwardOutput()
                .build();
    }

    @Override
    protected void after() {
        try {
            FileUtils.forceDelete(projectDir);
        } catch (IOException e) {
            throw new RuntimeException("Unable to remove temporary project directory " + projectDir, e);
        }
    }

    private void checkGradleCompatibility(JavaGradle javaGradle, GradleVersion testGradle) {
        JavaVersion javaVersion = javaGradle.javaVersion;
        GradleVersion minimalGradle = javaGradle.gradleVersion;
        JavaVersion currentJava = JavaVersion.current();
        boolean configurationSupported =
                // E.g. if the current Java is 12, then "Java 16 is supported in 7.0" does not help us, and we assume true
                currentJava.compareTo(javaVersion) < 0
                        // If the current Java is 12, and we know that "Java 11 was supported since Gradle 5.0",
                        // then we require current Gradle to be 5.0+
                        || testGradle.compareTo(minimalGradle) >= 0;
        if (configurationSupported) {
            return;
        }
        String skipMessage = "Java " + javaVersion.getMajorVersion() + " requires Gradle " + minimalGradle + "+, current Java is " + currentJava +
                ", test Gradle version is " + testGradle + ", so will skip the test";
        System.out.println(skipMessage);
        Assume.assumeTrue(skipMessage, configurationSupported);
    }

    private static File copyProject(String project) {
        try {
            File to = new File("build/gradle-testkit", randomAlphabetic(8));
            File from = new File(project);
            FileUtils.copyDirectory(from, to);
            return to;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<File> readPluginClasspath() throws IOException {
        try (InputStream stream = GradleRunnerRule.class.getClassLoader().getResourceAsStream("plugin-classpath.txt")) {
            return IOUtils.readLines(stream, StandardCharsets.UTF_8).stream()
                    .map(File::new).collect(Collectors.toList());
        }
    }

}
