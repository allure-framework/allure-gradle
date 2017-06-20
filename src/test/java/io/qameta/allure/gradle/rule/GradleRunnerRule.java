package io.qameta.allure.gradle.rule;

import io.qameta.allure.gradle.util.TestUtil;
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
import java.util.stream.Collectors;


/**
 * eroshenkoam
 * 20.06.17
 */
public class GradleRunnerRule extends ExternalResource {

    private final String[] tasks;

    private final String project;

    private BuildResult buildResult;

    private File projectDir;

    public GradleRunnerRule(String project, String... tasks) {
        this.project = project;
        this.tasks = tasks;
    }

    public BuildResult getBuildResult() {
        return this.buildResult;
    }

    public File getProjectDir() {
        return this.projectDir;
    }

    protected void before() throws Throwable {
        List<File> pluginClasspath = readPluginClasspath();

        projectDir = copyProject(project);

        buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withTestKitDir(new File(projectDir.getParentFile().getAbsolutePath(), ".gradle"))
                .withPluginClasspath(pluginClasspath)
                .withArguments(tasks)
                .build();
    }

    private static File copyProject(String dirName) throws IOException {
        File to = new File("build/gradle-testkit", dirName);
        File from = new File("src/it", dirName);
        FileUtils.cleanDirectory(to);
        FileUtils.copyDirectory(from, to);
        return to;
    }

    private static List<File> readPluginClasspath() throws IOException {
        try (InputStream stream = TestUtil.class.getClassLoader().getResourceAsStream("plugin-classpath.txt")) {
            return IOUtils.readLines(stream, StandardCharsets.UTF_8).stream()
                    .map(File::new).collect(Collectors.toList());
        }
    }

}
