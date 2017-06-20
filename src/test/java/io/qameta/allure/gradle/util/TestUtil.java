package io.qameta.allure.gradle.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class TestUtil {

    private TestUtil() {
    }

    public static File copyProject(String dirName) throws IOException {
        return copyProject(dirName, true, true);
    }


    public static File copyProject(String dirName, boolean clean, boolean copy) throws IOException {
        File to = new File("build/gradle-testkit", dirName);
        File from = new File("src/it", dirName);
        if (to.exists() && clean) {
            FileUtils.cleanDirectory(to);
        }
        if (copy) {
            FileUtils.copyDirectory(from, to);
        }
        return to;
    }

    public static List<File> readPluginClasspath() throws IOException {
        try (InputStream stream = TestUtil.class.getClassLoader().getResourceAsStream("plugin-classpath.txt")) {
            return IOUtils.readLines(stream, StandardCharsets.UTF_8).stream()
                    .map(File::new).collect(Collectors.toList());
        }
    }
}
