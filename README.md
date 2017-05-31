#Allure Gradle Plugin 

This plugin allows you to configure Allure reporting in your Gradle project
in one simple task that handles every aspect of Allure configuration.

As for now only TestNG-based projects are supported:

Sample configuration:

```groovy
plugins {
    id 'io.qameta.allure'
}

allure {
    testNG = true
    aspectjweaver = true
    allureVersion = '2.0.1'
    testNGAdapterVersion = '2.0-BETA8'
}
```

#Full configuration

```groovy
allure{
    allureVersion = '2.0.1'
    allureReportDir = "$buildDir/allure-report"
    allureResultsDir = "buildDir/allure-results"
    
    aspectjweaver = true
    aspectjVersion = "1.8.9"
    downloadLinkFormat = "https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/%s/allure-%<s.zip"
    
    testNGAdapterVersion = '2.0-BETA8'
    testNG = true
}
```