import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.report.AllureReportExtension
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the

val AllureExtension.report: AllureReportExtension get() = (this as ExtensionAware).the()

fun AllureExtension.report(configureAction: Action<in AllureReportExtension>) {
    configureAction.execute(report)
}
