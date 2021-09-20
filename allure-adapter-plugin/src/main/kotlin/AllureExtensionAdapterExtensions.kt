import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.adapter.AllureAdapterExtension
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the

val AllureExtension.adapter: AllureAdapterExtension get() = (this as ExtensionAware).the()

fun AllureExtension.adapter(configureAction: Action<in AllureAdapterExtension>) {
    configureAction.execute(adapter)
}
