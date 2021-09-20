package io.qameta.allure.gradle.adapter.autoconfigure

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

@CacheableTransform
abstract class TrimMetaInfServices53 : BaseTrimMetaInfServices() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputArtifact
    // Gradle 5.3 supports File inputs only
    abstract val inputArtifact: File

    override fun transform(outputs: TransformOutputs) {
        doTransform(inputArtifact, outputs)
    }
}
