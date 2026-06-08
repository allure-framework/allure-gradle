package io.qameta.allure.gradle.rule;

/**
 * Resolves the Gradle TestKit version used by integration tests.
 */
public final class GradleTestVersion {

    public static final String PROPERTY_NAME = "testGradleVersion";

    public static final String DEFAULT_VERSION = "9.5.1";

    private GradleTestVersion() {
    }

    public static String current() {
        String configured = System.getProperty(PROPERTY_NAME);
        String version = configured == null ? DEFAULT_VERSION : configured.trim();
        if (version.isEmpty()) {
            throw new IllegalArgumentException(
                    "System property " + PROPERTY_NAME + " must not be blank"
            );
        }
        return version;
    }
}
