package io.qameta.allure.gradle.adapter.autoconfigure

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Removes `META-INF/services` folder from a jar.
 * It enables to automatically transform `allure-junit5` to `allure-junit5:spi-off`.
 */
@CacheableTransform
abstract class BaseTrimMetaInfServices : TransformAction<TransformParameters.None> {
    companion object {
        val ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String::class.java)
        const val NO_SPI_JAR = "jar-no-spi"
    }

    protected fun doTransform(inputFile: File, outputs: TransformOutputs) {
        val outputFile = outputs.file(inputFile.name.removeSuffix(".jar") + "-spi-off.jar")

        outputFile.outputStream().buffered().use { outStream ->
            JarOutputStream(outStream).use { outFile ->
                JarFile(inputFile).use { inputJar ->
                    inputJar.stream()
                        .filter { !it.name.startsWith("META-INF/services") }
                        .forEachOrdered {
                            val entryStream = inputJar.getInputStream(it)
                            outFile.putNextEntry(it)
                            entryStream.copyTo(outFile)
                            outFile.closeEntry()
                        }
                }
            }
        }
    }
}
