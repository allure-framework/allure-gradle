Allure Gradle Plugin Sandbox
----------------------------

This is a sandbox project that simplifies Allure Gradle Plugin development.

You could try DSL in `dsl-groovy` and `dsl-kotlin` projects, and you see the full
IDE experience of the plugin.

The `allure-gradle` plugin is imported via Gradle's `includeBuild`, so
you do not need to build and publish the plugin before trying the plugin.

# Known issues

https://github.com/gradle/gradle/issues/16979

```
Execution failed for task ':xxxx-xxxxxx-build:spring-boot:generatePrecompiledScriptPluginAccessors'.
> Could not create service of type ScriptHandlerInternal using ProjectScopeServices.createScriptHandler().
   > Cannot create service of type DependencyLockingHandler using method DefaultDependencyManagementServices$DependencyResolutionScopeServices.createDependencyLockingHandler() as there is a problem with parameter #2 of type ConfigurationContainerInternal.
      > Cannot create service of type ConfigurationContainerInternal using method DefaultDependencyManagementServices$DependencyResolutionScopeServices.createConfigurationContainer() as there is a problem with parameter #2 of type ConfigurationResolver.
         > Cannot create service of type ConfigurationResolver using method DefaultDependencyManagementServices$DependencyResolutionScopeServices.createDependencyResolver() as there is a problem with parameter #1 of type ArtifactDependencyResolver.
            > Cannot create service of type ArtifactDependencyResolver using method DependencyManagementBuildScopeServices.createArtifactDependencyResolver() as there is a problem with parameter #4 of type List<ResolverProviderFactory>.
               > Could not create service of type VersionControlRepositoryConnectionFactory using VersionControlBuildSessionServices.createVersionControlSystemFactory().
                  > Failed to create parent directory '/.gradle' when creating directory '/.gradle/vcs-1'
```

Solution: run Gradle task manually

