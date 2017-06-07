package io.qameta.allure.util

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class BuildUtils {

    static String getAllureExecutable() {
        return System.getProperty('os.name').toLowerCase().indexOf('win') >= 0 ? 'allure.bat'
                : 'allure'
    }
}
