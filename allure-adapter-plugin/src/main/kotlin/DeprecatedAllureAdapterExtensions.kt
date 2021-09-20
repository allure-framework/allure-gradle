import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.adapter.config.AdapterConfig
import org.gradle.api.Action

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use adapters.cucumberJvm",
    replaceWith = ReplaceWith("adapters.cucumberJvm.configure(action)")
)
fun AllureExtension.useCucumberJVM(action: Action<in AdapterConfig>) {
    adapter.adapters {
        action.execute(cucumberJvm)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use adapters.cucumber2Jvm",
    replaceWith = ReplaceWith("adapters.cucumber2Jvm { }")
)
fun AllureExtension.useCucumber2JVM(action: Action<in AdapterConfig>) {
    adapter.adapters {
        action.execute(cucumber2Jvm)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use adapters.junit4",
    replaceWith = ReplaceWith("adapters.junit4 { }")
)
fun AllureExtension.useJUnit4(action: Action<in AdapterConfig>) {
    adapter.adapters {
        action.execute(junit4)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use adapters.junit4",
    replaceWith = ReplaceWith("adapters.junit5 { }")
)
fun AllureExtension.useJUnit5(action: Action<in AdapterConfig>) {
    adapter.adapters {
        action.execute(junit5)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use adapters.testng",
    replaceWith = ReplaceWith("adapters.testng { }")
)
fun AllureExtension.useTestNG(action: Action<in AdapterConfig>) {
    adapter.adapters {
        action.execute(testng)
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING, message = "Use adapters.spock",
    replaceWith = ReplaceWith("adapters.spock { }")
)
fun AllureExtension.useSpock(action: Action<in AdapterConfig>) {
    adapter.adapters {
        action.execute(spock)
    }
}
