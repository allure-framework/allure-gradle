package io.qameta.allure.gradle.util

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class BuildUtils {

    static boolean isWindows() {
        return System.getProperty('os.name').toLowerCase().contains('win')
    }

    static String getAllureExecutable() {
        return isWindows() ? 'allure.bat' : 'allure'
    }

}
