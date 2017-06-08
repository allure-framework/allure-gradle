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
    resultsFolder = 'build/allure-results'
    allureJavaVersion = '2.0-BETA9'
    downloadLink = 'https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/2.0.1/allure-2.1.0.zip'
    
    resultsGlob = {
       include = '/path/to/project/**/build/**/allure-results'
       exclude = '/path/to/project/some-project/build'
    }
    resultsDirs = ['/path/to/project/module1/build/allure-results', 
    '/path/to/project/module2/build/allure-results']
    
    useJunit4{
       adapterVersion = '2.0-BETA9'
    }
    
    useTestNG {
       adapterVersion = '2.0-BETA9'
    }
    
    useCucumberJVM {
       adapterVersion = '2.0-BETA9'
    }
}
```

## Tasks

### `allure` 

Creates Allure report for a single-module project

### `aggregatedAllureReport`

Creates Allure report for a multi-module project, collection results from `resultsDirs` or `resultsGlob` if
 they are specified, or from parent project children that have folders with Allure results.