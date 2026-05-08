package io.qameta.allure.gradle.rule;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GradleRunnerRuleTest {

    @Test
    void nestedGradleEnvironmentDoesNotInheritAllureTestPlanFilters() {
        Map<String, String> environment = new HashMap<>();
        environment.put("ALLURE_TESTPLAN_PATH", "outer-allure-testplan.json");
        environment.put("AS_TESTPLAN_PATH", "outer-as-testplan.json");
        environment.put("PATH", "keep-me");

        Map<String, String> nestedEnvironment = GradleRunnerRule.nestedGradleEnvironment(environment);

        assertFalse(nestedEnvironment.containsKey("ALLURE_TESTPLAN_PATH"));
        assertFalse(nestedEnvironment.containsKey("AS_TESTPLAN_PATH"));
        assertEquals("keep-me", nestedEnvironment.get("PATH"));
    }
}
