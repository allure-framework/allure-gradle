package io.qameta.allure.gradle.util

import org.apache.commons.io.FileUtils

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class TestUtil {

     static File copyDataFiles(String dirName, boolean clean = true, boolean copy = true){
        def to = new File("build/gradle-testkit", dirName)
        def from = new File("src/it", dirName)
        if (to.exists() && clean) {
            FileUtils.cleanDirectory(to)
        }
        if (copy) {
            FileUtils.copyDirectory(from, to)
        }
        return to
    }

     static List<File> prepareClasspathFile(){
        def resource = TestUtil.class.classLoader.getResource("plugin-classpath.txt")
        if (resource == null) {
            throw new IllegalStateException(
                    "Did not find plugin classpath resource, run `testClasses` build task.")
        }
        resource.readLines().collect { new File(it) } 
    }
}
