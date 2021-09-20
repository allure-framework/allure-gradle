package io.qameta.allure.gradle.adapter.autoconfigure

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CacheableTransform
abstract class TrimMetaInfServices54 : BaseTrimMetaInfServices() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputArtifact
    // Provider<FileSystemLocation> is Gradle 5.4+
    abstract val inputFile: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        doTransform(inputFile.get().asFile, outputs)
    }
}
