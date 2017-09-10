[build]: https://ci.qameta.io/job/allure-gradle/job/master "Build"
[build-badge]: https://ci.qameta.io/buildStatus/icon?job=allure-gradle/master

[release]: https://github.com/allure-framework/allure-gradle/releases/latest "Release"
[release-badge]: https://img.shields.io/github/release/allure-framework/allure-gradle.svg

[bintray]: https://bintray.com/qameta/maven/allure-gradle "Bintray"
[bintray-badge]: https://img.shields.io/bintray/v/qameta/maven/allure-gradle.svg?style=flat

# Allure plugin for Gradle [![build-badge][]][build] [![release-badge][]][release] [![bintray-badge][]][bintray]

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
        classpath "io.qameta.allure:allure-gradle:<latest>"
    }
}

apply plugin: 'io.qameta.allure'

allure {
    autoconfigure = true
    version = '2.3.5'
}
```

## Full configuration

```groovy
allure {
    version = '2.3.5'
    aspectjweaver = true
    autoconfigure = true

    resultsGlob = {
       include '/path/to/project/**/build/**/allure-results'
       exclude '/path/to/project/some-project/build'
    }
    
    resultsDir = file('/path/to/project/module1/build/allure-results')
    reportDir = file('build/allure-results')
    
    useJunit4 {
       version = '2.0-BETA10'
    }
    
    useJunit5 {
       version = '2.0-BETA10'
    }

    useTestNG {
       version = '2.0-BETA10'
    }
    
    useCucumberJVM {
       version = '2.0-BETA10'
    }
    
    useSpock {
       version = '2.0-BETA10'
    }
    
    downloadLink = 'https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/2.1.1/allure-2.1.1.zip'
}
```
`autoconfigure` *boolean* - a flag to specify usage of autoconfiguration, plugin will attempt to find test platform 
integration provided by Gradle (currently works only for Junit4 and TestNG) 

`aspectjveaver` *boolean* - a flag to specify inclusion/exclusion of aspectjweaver runtime agent

`clean` *boolean* - enable `--clean` option for the Allure commandline tool

`version` *String* - specify report generator version, note, this property is necessary to enable `allure` and 
`aggregatedAllureReport` tasks

`configuration` *String* (`default = 'testCompile'`) - configuration name where to bind plugin dependencies

`resultsDir` *File* - directory for Allure results in the current project, `build\allure-results` by default

`reportDir` *File* - directory for Allure results in the current project, `build\allure-results` by default

`allureJavaVersion` *String* - version of allure java release to be used for autoconfiguration

`downloadLink` *String* - custom location of Allure distribution to download from, by default allure is downloaded from 
bintray by sspecified version and installed to `.allure` folder in the project root.

## Tasks

### `allureReport` 

Creates Allure report for a single-module project

### `allureServe`
Creates Allure report for a single-module project in the tmp folder and opens it in the default browser.
