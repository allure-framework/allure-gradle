import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.download.AllureCommandlineExtension
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the

val AllureExtension.commandline: AllureCommandlineExtension get() = (this as ExtensionAware).the()

fun AllureExtension.commandline(configureAction: Action<in AllureCommandlineExtension>) {
    configureAction.execute(commandline)
}
