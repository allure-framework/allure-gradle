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
    version = '2.1.0'
    aspectjweaver = true
    autoconfigure = true

    resultsGlob = {
       include '/path/to/project/**/build/**/allure-results'
       exclude '/path/to/project/some-project/build'
    }
    
    resultsDir = file('/path/to/project/module1/build/allure-results')
    reportDir = file('build/allure-results')
    
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
    
    downloadLink = 'https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/2.0.1/allure-2.1.0.zip'
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
