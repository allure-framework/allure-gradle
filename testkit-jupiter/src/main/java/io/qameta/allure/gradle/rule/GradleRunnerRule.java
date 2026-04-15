package io.qameta.allure.gradle.rule;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.JavaVersion;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.commons.lang3.RandomStringUtils.insecure;

/**
 * Historical helper retained for Gradle TestKit setup while the suite uses JUnit Jupiter.
 */
public class GradleRunnerRule {

    private static final File DEFAULT_ROOT_DIR = new File("build/gradle-testkit");

    private String project;

    private String[] tasks = new String[0];

    private String gradleVersion = "9.0.0";

    private File rootDir = DEFAULT_ROOT_DIR;

    private BuildResult buildResult;

    private File projectDir;

    private File testKitDir;

    public BuildResult getBuildResult() {
        return this.buildResult;
    }

    public File getProjectDir() {
        return this.projectDir;
    }

    public File getTestKitDir() {
        return this.testKitDir;
    }

    public GradleRunnerRule version(String version) {
        this.gradleVersion = version;
        return this;
    }

    public GradleRunnerRule project(String project) {
        this.project = project;
        return this;
    }

    public GradleRunnerRule rootDir(File rootDir) {
        this.rootDir = rootDir;
        return this;
    }

    public GradleRunnerRule tasks(String... tasks) {
        this.tasks = tasks;
        return this;
    }

    public GradleRunnerRule prepare() {
        if (projectDir != null) {
            return this;
        }
        if (project == null || project.isEmpty()) {
            throw new IllegalStateException("Gradle test project path must be configured");
        }
        rootDir.mkdirs();
        projectDir = copyProject(rootDir, project);
        ensureSettingsFile(projectDir);
        testKitDir = testKitDirFor(projectDir);
        checkGradleCompatibility();
        return this;
    }

    public GradleRunnerRule build() {
        prepare();
        buildResult = newRunner(tasks).build();
        return this;
    }

    public BuildResult run(String... tasks) {
        prepare();
        buildResult = newRunner(tasks).build();
        return buildResult;
    }

    public GradleRunner newRunner(String... arguments) {
        prepare();
        List<String> args = new ArrayList<>();
        args.add("--stacktrace");
        args.add("--info");
        // --no-daemon does not work with GradleRunner
        args.add("-Porg.gradle.daemon=false");
        GradleVersion testGradle = GradleVersion.version(gradleVersion);
        if (testGradle.compareTo(GradleVersion.version("7.0")) >= 0) {
            // Disable file watching since it prevents file removal on Windows
            // See https://github.com/gradle/gradle/pull/16977
            // FS watch is disabled to avoid test flakiness
            args.add("--no-watch-fs");
        }
        args.addAll(Arrays.asList(arguments));

        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .withTestKitDir(testKitDir)
                .withPluginClasspath()
                .forwardOutput();
    }

    static class JavaGradle {
        final JavaVersion javaVersion;
        final GradleVersion gradleVersion;

        JavaGradle(JavaVersion javaVersion, String gradleVersion) {
            this.javaVersion = javaVersion;
            this.gradleVersion = GradleVersion.version(gradleVersion);
        }
    }

    private void checkGradleCompatibility() {
        GradleVersion testGradle = GradleVersion.version(gradleVersion);

        // Configuration avoidance tasks.register requires Gradle 4.9+
        if (testGradle.compareTo(GradleVersion.version("8.11")) < 0) {
            Assertions.fail("allure-gradle plugin requires Gradle 8.11+, the can't launch tests with Gradle " + testGradle);
        }

        Optional<JavaGradle> gradleRequirement = Stream.of(
                        new JavaGradle(JavaVersion.VERSION_24, "8.14"),
                        new JavaGradle(JavaVersion.VERSION_21, "8.5"),
                        new JavaGradle(JavaVersion.VERSION_17, "7.3"),
                        new JavaGradle(JavaVersion.VERSION_16, "7.0"),
                        new JavaGradle(JavaVersion.VERSION_15, "6.7"),
                        new JavaGradle(JavaVersion.VERSION_14, "6.3")
                )
                .filter(v -> v.javaVersion.compareTo(JavaVersion.current()) <= 0)
                .findFirst();
        if (!gradleRequirement.isPresent()) {
            throw new IllegalStateException("Running with Java " + JavaVersion.current().getMajorVersion() + " is not supported yet");
        }

        checkGradleCompatibility(gradleRequirement.get(), testGradle);
    }

    private void checkGradleCompatibility(JavaGradle javaGradle, GradleVersion testGradle) {
        JavaVersion javaVersion = javaGradle.javaVersion;
        GradleVersion minimalGradle = javaGradle.gradleVersion;
        JavaVersion currentJava = JavaVersion.current();
        boolean configurationSupported =
                currentJava.compareTo(javaVersion) < 0
                        || testGradle.compareTo(minimalGradle) >= 0;
        if (configurationSupported) {
            return;
        }
        String skipMessage = "Java " + javaVersion.getMajorVersion() + " requires Gradle " + minimalGradle + "+, current Java is " + currentJava +
                ", test Gradle version is " + testGradle + ", so will skip the test";
        System.out.println(skipMessage);
        Assumptions.assumeTrue(configurationSupported, skipMessage);
    }

    private static void ensureSettingsFile(File projectDir) {
        File settingsGradle = new File(projectDir, "settings.gradle");
        File settingsGradleKts = new File(projectDir, "settings.gradle.kts");
        if (settingsGradle.exists() || settingsGradleKts.exists()) {
            return;
        }
        try {
            File buildFileKts = new File(projectDir, "build.gradle.kts");
            File settingsFile = buildFileKts.exists() ? settingsGradleKts : settingsGradle;
            settingsFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create settings file in " + projectDir, e);
        }
    }

    public static File testKitDirFor(File projectDir) {
        File absoluteProjectDir = projectDir.getAbsoluteFile();
        File parentDir = Optional.ofNullable(absoluteProjectDir.getParentFile())
                .map(File::getParentFile)
                .orElse(null);
        if (parentDir == null) {
            parentDir = new File(System.getProperty("java.io.tmpdir"));
        }
        String suffix = Integer.toUnsignedString(absoluteProjectDir.getAbsolutePath().hashCode());
        return new File(parentDir, ".gradle-test-kit-" + absoluteProjectDir.getName() + "-" + suffix);
    }

    private static File copyProject(File rootDir, String project) {
        String projectName = StringUtils.substringAfterLast(project.replace('\\', '/'), '/');
        if (!projectName.isEmpty()) {
            projectName += "-";
        }
        File to = new File(rootDir, projectName + insecure().nextAlphabetic(8));
        File from = new File(project);
        try {
            if (!from.isDirectory() || !from.exists()) {
                throw new IllegalArgumentException("Directory " + project + " is not found " +
                        "(full path is " + from.getAbsolutePath() + ")");
            }
            FileUtils.copyDirectory(from, to);
            return to;
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy " + project +
                    " (full path is " + from.getAbsolutePath() + ") to " + to, e);
        }
    }
}
