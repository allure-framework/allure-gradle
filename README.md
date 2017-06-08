# Allure plugin for Gradle

Now Allure Plugin allows you to integrate 
[Allure](https://docs.qameta.io/allure/latest/) into TestNG, Junit4 and Cucumber JVM gradle projects

## Basic usage

this configuration will use gradle integration for Junit4 and TestNG and generate report for a single-module project

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "io.qameta.allure:allure-gradle:2.0-BETA1"
    }
}

apply plugin: 'io.qameta.allure'

allure {
    autoconfigure = true
    version = '2.1.0'
}
```

## Full configuration

```groovy
allure {
    aspectjweaver = true
    clean = true
    version = '2.1.0'
    configuration = 'testCompile'
    resultsDirectory = 'build/allure-results'
    allureJavaVersion = '2.0-BETA9'
    downloadLink = 'https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/2.0.1/allure-2.1.0.zip'
    
    resultsGlob = {
       include '/path/to/project/**/build/**/allure-results'
       exclude '/path/to/project/some-project/build'
    }
    
    resultsDirectories = ['/path/to/project/module1/build/allure-results', 
    '/path/to/project/module2/build/allure-results']
    
    useJunit4 {
       version = '2.0-BETA9'
    }
    
    useTestNG {
       version = '2.0-BETA9'
    }
    
    useCucumberJVM {
       version = '2.0-BETA9'
    }
    
    useSpock {
       version = '2.0-BETA9'
    }
}
```
`autoconfigure` *boolean* - a flag to specify usage of autoconfiguration, plugin will attempt to find test platform 
integration provided by Gradle (currently works only for Junit4 and TestNG) 

`aspectjveaver` *boolean* - a flag to specify inclusion/exclusion of aspectjweaver runtime agent

`clean` *boolean* - enable `--clean` option for the Allure commandline tool

`version` *String* - specify report generator version, note, this property is necessary to enable `allure` and 
`aggregatedAllureReport` tasks

`configuration` *String* (`default = 'testCompile'`) - configuration name where to bind plugin dependencies

`resultsDirectory` *String* - directory for Allure results in the current project, `build\allure-results` by default

`allureJavaVersion` *String* - version of allure java release to be used for autoconfiguration

`downloadLink` *String* - custom location of Allure distribution to download from, by default allure is downloaded from 
bintray by sspecified version and installed to `.allure` folder in the project root.

`resultsGlob` *Closure* - closure to configure FileTree with results directories a base directory of the tree is 
in the project's root. See FileTree [documentation](https://docs.gradle.org/current/userguide/working_with_files.html)
supports `include` and `exclude` patterns.

`resultsDirectories` *List* - list of Allure results directories to be used for `aggregatedAllureReport` task

## Tasks

### `allure` 

Creates Allure report for a single-module project

### `aggregatedAllureReport`

Creates Allure report for a multi-module project, collection results from `resultsDirs` or `resultsGlob` if
 they are specified, or from parent project children that have folders with Allure results.
 
### `serve`
Creates Allure report for a single-module project in the tmp folder and opens it in the default browser.