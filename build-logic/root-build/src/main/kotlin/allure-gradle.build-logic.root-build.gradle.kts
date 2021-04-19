import io.github.gradlenexus.publishplugin.NexusPublishExtension

plugins {
    base
//    id("net.researchgate.release")
}

//project.release {
//    tagTemplate = version.toString()
//}

// io.github.gradle-nexus.publish-plugin is not compatible with precompiled Gradle plugins,
// so the plugin is applied in the root project, and here we configure it
extensions.findByType<NexusPublishExtension>()?.apply {
    repositories {
        sonatype()
    }
}
