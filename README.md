[release]: https://github.com/allure-framework/allure-gradle/releases/latest "Release"
[release-badge]: https://img.shields.io/github/release/allure-framework/allure-gradle.svg

# Allure plugin for Gradle [![Build](https://github.com/allure-framework/allure-gradle/actions/workflows/build.yml/badge.svg)](https://github.com/allure-framework/allure-gradle/actions/workflows/build.yml) [![release-badge][]][release]

Gradle projects plugins for building [Allure](https://docs.qameta.io/allure/latest/) reports for TestNG, JUnit4, JUnit5, Cucumber JVM, and Spock tests.

## Basic usage

`allure-gradle` plugin implements Allure data collecting (e.g. Test` tasks), and data reporting (both individual and aggregate reports).

Data colecting and reporting are split to different Gradle plugins, so you could apply the ones you need.

Note:
* allure-gradle 2.9+ requires Gradle 5.0+
* allure-gradle 2.11+ requires Gradle 6.0+

The minimal configuration is as follows.
It would configure test tasks to collect Allure results and add `allureReport` and `allureServe`
tasks for report inspection.

Groovy DSL:

```groovy
plugins {
    id'io.qameta.allure' version '<latest>'
}

repositories {
    // Repository is needed for downloading allure-commandline for building the report
    mavenCentral()
}
```

Kotlin DSL:

```kotlin
plugins {
    id("io.qameta.allure") version "<latest>"
}

repositories {
    // Repository is needed for downloading allure-commandline for building the report
    mavenCentral()
}
```

`io.qameta.allure` is a shortcut for `io.qameta.allure-adapter` + `io.qameta.allure-report`,
so you could apply the plugins you need.

### Configuring Allure version

Groovy DSL:

```groovy
allure {
    version = "2.30.0"
}
```

Kotlin DSL:

```kotlin
allure {
    version = "2.30.0"
}
```

### Building Allure report

To build a report, and browse it use the following command:

    ./gradlew allureServe

Note: by default, `allureServe` does not execute tests, so if you want to execute the relevant
tests and build report, use the following:

    ./gradlew allureReport --depends-on-tests

To build an aggregate report, and browse it, apply `io.qameta.allure-aggregate-report` plugin and
use the following command:

    ./gradlew allureAggregateServe

If you need a report only, please use `allureReport` and `allureAggregateReport`.

By default, `allureAggregate*` aggregates data from the current `project` and its `subprojects`.
However, you need to apply `io.qameta.allure-adapter` plugin to the relevant subprojects, so they
provide Allure results.

## Customizing data collecting

Data collecting is implemented via `io.qameta.allure-adapter` Gradle plugin.

The values in the sample below are the defaults.
The sample uses Kotlin DSL. In Groovy DSL you could use `allureJavaVersion = "2.19.0"`, however, that is the only difference.

```kotlin
allure {
    version.set("2.30.0")
    adapter {
        // Configure version for io.qameta.allure:allure-* adapters
        // It defaults to allure.version
        allureJavaVersion.set("2.28.0")
        aspectjVersion.set("1.9.22.1")

        // Customize environment variables for launching Allure
        environment.put("JAVA_HOME", "/path/to/java_home")

        autoconfigure.set(true)
        autoconfigureListeners.set(true)
        aspectjWeaver.set(true)

        // By default, categories.json is detected in src/test/resources/../categories.json,
        // However, it would be better to put the file in a well-known location and configure it explicitly
        categoriesFile.set(layout.projectDirectory.file("config/allure/categories.json"))
        frameworks {
            junit5 {
                // Defaults to allureJavaVersion
                adapterVersion.set("...")
                enabled.set(true)
                // Enables allure-junit4 default test listeners via META-INF/services/...
                autoconfigureListeners.set(true)
            }
            junit4 {
                // same as junit5
            }
            testng {
                // same as junit5
            }
            spock
            cucumberJvm
            // Alternative syntax: cucumberJvm(2) {...}
            cucumber2Jvm
            cucumber3Jvm
            cucumber4Jvm
            cucumber5Jvm
            cucumber6Jvm
        }
    }
}
```

### What if I have both JUnit5, JUnit4, and CucumberJVM on the classpath?

By default, `allure-gradle` would detect all of them and apply all the listeners yielding 3 reports.
If you need only one or two, specify the required ones via `frameworks {...}` block.

### Adding custom results for reporting

You could add a folder with custom results via `allureRawResultElements` Gradle configuration.

```kotlin
plugins {
    id("io.qameta.allure-adapter-base")
}

dependencies {
    allureRawResultElements(files(layout.buildDirectory.dir("custom-allure-results")))
    // or
    allureRawResultElements(files("$buildDir/custom-allure-results"))
}

// If the results are built with a task, you might want adding a dependency so aggregate report
// knows which tasks to run before building the report
allureRawResultElements.outgoing.artifact(file("...")) {
    builtBy(customTask)
}
```

### Using custom JUnit5 listeners instead of the default ones

`allure-java` comes with a set of default listeners for JUnit4, JUnit5, and TestNG.
However, you might want to disable them and use your own ones.

Here's how you disable default listeners:

```kotlin
allure.adapter.frameworks.junit5.autoconfigureListeners.set(false)
```

An alternative syntax is as follows:

```kotlin
allure {
    adapter {
        frameworks {
            // Note: every time you mention an adapter, it is added to the classpath,
            // so refrain from mentioning unused adapters here
            junit5 {
                // Disable allure-junit5 default test listeners
                autoconfigureListeners.set(false)
            }
            testng {
                // Disable allure-testng default test listeners
                autoconfigureListeners.set(false)
            }
        }
    }
}
```

## Report generation

### Aggregating results from multiple projects

Suppose you have a couple of modules `/module1/build.gradle.kts`,
`/module2/build.gradle.kts` that collect raw results for Allure:

```kotlin
// Each submodule
plugin {
    `java-library`
    id("io.qameta.allure-adapter")
}

allure {
    adapter {
        frameworks {
            junit5
        }
    }
}

// Each Test task will write raw data for Allure automatically
```

Here's how you can aggregate that in their parent project (e.g. `root` project):

`/build.gradle.kts`

```kotlin
plugin {
    id("io.qameta.allure-aggregate-report")
}

// allure-aggregate-report requires allure-commandline, so we need a repository here
repositories {
    mavenCentral()
}
```

Browse report:

    ./gradlew allureAggregateServe

By default `io.qameta.allure-aggregate-report` would aggregate results
from `allprojects` (==current project + its subprojects), however,
you can configure the set of modules as follows:

```kotlin
// By default, aggregate-report aggregates allprojects (current + subprojects)
// So we want to exclude module3 since it has no data for Allure
configurations.allureAggregateReport.dependencies.remove(
        project.dependencies.create(project(":module3"))
)

// Removing the default allprojects:
configurations.allureAggregateReport.dependencies.clear()

// Adding a custom dependency
dependencies {
    allureAggregateReport(project(":module3"))
}
```

### Customizing report folders

Report generation is implemented via `io.qameta.allure-report` Gradle plugin, so if you need reports,
apply the plugin as follows:

```kotlin
plugins {
    id("io.qameta.allure-report")
}
```

By default, the report is produced into Gradle's default reporting folder under `task.name` subfolder:

   $buildDir/reports/allure-report/allureReport
   $buildDir/reports/allure-report/allureAggregateReport

You could adjust the default location as follows:
```kotlin
plugins {
    id("io.qameta.allure-report") // the plugin is packaged with Gradle by default
}

// See https://docs.gradle.org/current/dsl/org.gradle.api.reporting.ReportingExtension.html
// Extension is provided via Gradle's `reporting-base` plugin
reporting {
    baseDir = "$buildDir/reports"
}

allure {
    report {
        // There might be several tasks producing the report, so the property
        // configures a base directory for all the reports
        // Each task creates its own subfolder there
        reportDir.set(project.reporting.baseDirectory.dir("allure-report"))
    }
}
```

### Running tests before building the report

By default, `allureReport` task will NOT execute tests.
This enables trying new `categories.json` faster, however, if you need to see the latest results, the following
might help:

* Execute tests separately: `./gradlew test`
* Use `--depends-on-tests` as follows (the option should come after the task name): `./gradlew allureReport --depends-on-tests`
* Configure `allure.report.dependsOnTest.set(true)`

```kotlin
allure {
    report {
        // By default, allureReport will NOT execute tests
        // If the tests are fast (e.g. UP-TO-DATE or FROM-CACHE),
        // then you might want configure dependsOnTests.set(true) so you always
        // get the latest report from allureReport
        dependsOnTests.set(false)
    }
}
```

### Customizing allure-commandline download

Allure download is handled with `io.qameta.allure-download` plugin which adds `allureDownload` task.
Typically, applying `io.qameta.allure-report` is enough, however, you could use `io.qameta.allure-download`
if you do not need reporting and you need just a fresh `allure-commandline` binary.

By default `allure-commandline` is downloaded from Sonatype OSSRH (also known as Maven Central).

The plugin receives `allure-commandline` via `io.qameta.allure:allure-commandline:$version@zip` dependency.

If you have a customized version, you could configure it as follows:

```kotlin
allure {
    // This configures the common Allure version, so it is used for commandline as well
    version.set("2.30.0")

    commandline {
        // The following patterns are supported: `[group]`, `[module]`, `[version]`, `[extension]`
        // The patterns can appear severs times if you need
        // By default, downloadUrlPattern is NOT set.
        downloadUrlPattern.set("https://server/path/[group]/[module]-[version].[extension]")

        // groupId for allure-commandline
        group.set("io.qameta.allure")
        // module for allure-commandline
        module.set("allure-commandline")
        // extension for allure-commandline
        extension.set("zip")
    }
}
```

Note: if you configure `downloadUrlPattern`, then `io.qameta.allure-download` plugin configures
an extra `ivy` repository with the provided URL, and it uses `custom.io.qameta.allure:allure-commandline:...`
coordinates to identify custom distribution is needed.

If you use Gradle 6.2+, then the custom repository is configured with `exclusive content filtering`
which means the repository would be used exclusively for `custom.io.qameta.allure:allure-commandline`.

If you use Gradle 5.1+, then the repository would be configured with regular filtering, so it would be
slightly less secure and slightly less efficient.

### Using local allure-commandline binary

`allure-commandline` is resolved via `allureCommandline` configuration, so you could configure
local file as follows.

Remember: NEVER use relative paths in your build files since "current directory" does not exist
in a multi-threaded project execution (see https://youtrack.jetbrains.com/issue/IDEA-265203#focus=Comments-27-4795223.0-0).

```kotlin
dependencies {
    // allureCommandline must resolve to a single zip file
    // You could use regular Gradle syntax to specify the dependency
    allureCommandline(files("/path/to/allure-commandline.zip"))
}
```

## Technical details

### io.qameta.allure-base plugin

Extensions:
* io.qameta.allure.gradle.base.AllureExtension

  `allure` extension for `project`

### io.qameta.allure-adapter-base plugin

Extensions:
* io.qameta.allure.gradle.adapter.AllureAdapterExtension

  `adapter` extension for `allure`

Configurations:
* `allureRawResultElements`

  A consumable configuration that exposes the collect raw data for building the report

Tasks:
* `copyCategories: io.qameta.allure.gradle.adapter.tasks.CopyCategories`

  Copies `categories.json` to the raw results folders.
  See https://github.com/allure-framework/allure2/issues/1236

### io.qameta.allure-adapter plugin

Configures automatic collectint of raw data from test tasks, adds `allure-java` adapters to the classpath.

Configurations:
* `allureAspectjWeaverAgent`

  A configuration to declare AspectJ agent jar for data collecting

### io.qameta.allure-download plugin

Downloads and unpacks `allure-commandline`

Extensions:
* `io.qameta.allure.gradle.download.AllureCommandlineExtension`

  `commandline` extension for `allure`

Configurations:
* `allureCommandline`

  A configuration to resolve `allure-commandline` zip

Tasks:
* `allureDownload: io.qameta.allure.gradle.download.tasks.DownloadAllure`

  Retrieves and unpacks `allure-commandline`

### io.qameta.allure-report-base plugin

Applies `reporting-base` plugin and adds `allure.report` extension.

Extensions:
* `io.qameta.allure.gradle.report.AllureReportExtension`

  `report` extension for `allure`

### io.qameta.allure-report plugin

Builds Allure report for the current project.

Configurations:
* `allureReport`

  Note: prefer exposing raw results via `allureRawResultElements` configuration
  rather than declaring them in `allureReport` configuration.

Tasks:
* `allureReport: io.qameta.allure.gradle.report.tasks.AllureReport`

  Builds Allure report for the current project

* `allureServe: io.qameta.allure.gradle.report.tasks.AllureServe`

  Launches a web server for browsing Allure report

### io.qameta.allure-aggregate-report plugin

Builds Allure aggregate report.

Configurations:
* `allureAggregateReport`

  A configuration for declaring projects to aggregate the results from.
  Each project exposes its raw results via `allureRawResultElements` configuration.

Tasks:
* `allureAggregateReport: io.qameta.allure.gradle.report.tasks.AllureReport`

  Builds Allure aggregate report

* `allureAggregateServe: io.qameta.allure.gradle.report.tasks.AllureServe`

  Launches a web server for browsing Allure aggregate report
