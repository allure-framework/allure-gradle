import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.adapter.config.AdapterConfig
import org.gradle.api.Action

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use frameworks.cucumberJvm",
    replaceWith = ReplaceWith("frameworks.cucumberJvm.configure(action)")
)
fun AllureExtension.useCucumberJVM(action: Action<in AdapterConfig>) {
    adapter.frameworks {
        action.execute(cucumberJvm)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use frameworks.cucumber2Jvm",
    replaceWith = ReplaceWith("frameworks.cucumber2Jvm { }")
)
fun AllureExtension.useCucumber2JVM(action: Action<in AdapterConfig>) {
    adapter.frameworks {
        action.execute(cucumber2Jvm)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use frameworks.junit4",
    replaceWith = ReplaceWith("frameworks.junit4 { }")
)
fun AllureExtension.useJUnit4(action: Action<in AdapterConfig>) {
    adapter.frameworks {
        action.execute(junit4)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use frameworks.junit4",
    replaceWith = ReplaceWith("frameworks.junit5 { }")
)
fun AllureExtension.useJUnit5(action: Action<in AdapterConfig>) {
    adapter.frameworks {
        action.execute(junit5)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use frameworks.testng",
    replaceWith = ReplaceWith("frameworks.testng { }")
)
fun AllureExtension.useTestNG(action: Action<in AdapterConfig>) {
    adapter.frameworks {
        action.execute(testng)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use frameworks.spock",
    replaceWith = ReplaceWith("frameworks.spock { }")
)
fun AllureExtension.useSpock(action: Action<in AdapterConfig>) {
    adapter.frameworks {
        action.execute(spock)
    }
}
