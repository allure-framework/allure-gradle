package io.qameta.allure.gradle.rule;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.rules.ExternalResource;

import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;


/**
 * eroshenkoam
 * 20.06.17
 */
public class GradleRunnerRule extends ExternalResource {

    private final Provider<String> projectProvider;

    private final String[] tasks;

    private BuildResult buildResult;

    private File projectDir;

    public GradleRunnerRule(Provider<String> projectProvider, String... tasks) {
        this.projectProvider = projectProvider;
        this.tasks = tasks;
    }

    public GradleRunnerRule(String project, String... tasks) {
        this(() -> project, tasks);
    }

    public BuildResult getBuildResult() {
        return this.buildResult;
    }

    public File getProjectDir() {
        return this.projectDir;
    }

    protected void before() throws Throwable {
        List<File> pluginClasspath = readPluginClasspath();

        projectDir = copyProject(projectProvider.get());

        buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withTestKitDir(new File(projectDir.getParentFile().getAbsolutePath(), ".gradle"))
                .withPluginClasspath(pluginClasspath)
                .withArguments(tasks)
                .build();
    }

    private static File copyProject(String project) throws IOException {
        File to = new File("build/gradle-testkit", randomAlphabetic(8));
        File from = new File(project);
        FileUtils.copyDirectory(from, to);
        return to;
    }

    private static List<File> readPluginClasspath() throws IOException {
        try (InputStream stream = GradleRunnerRule.class.getClassLoader().getResourceAsStream("plugin-classpath.txt")) {
            return IOUtils.readLines(stream, StandardCharsets.UTF_8).stream()
                    .map(File::new).collect(Collectors.toList());
        }
    }

}
