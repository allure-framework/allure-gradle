import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.gather.AllureGatherExtension
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the

val AllureExtension.gather: AllureGatherExtension get() = (this as ExtensionAware).the()

fun AllureExtension.gather(configureAction: Action<in AllureGatherExtension>) {
    configureAction.execute(gather)
}
