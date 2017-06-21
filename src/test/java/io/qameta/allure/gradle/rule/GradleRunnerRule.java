package io.qameta.allure.gradle.rule;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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


    protected void before() throws Throwable {
        projectDir = copyProject(projectSupplier.get());
        buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(tasksSupplier.get())
                .withGradleVersion(versionSupplier.get())
                .withTestKitDir(new File(projectDir.getParentFile().getAbsolutePath(), ".gradle"))
                .withPluginClasspath(readPluginClasspath())
                .forwardOutput()
                .build();
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
