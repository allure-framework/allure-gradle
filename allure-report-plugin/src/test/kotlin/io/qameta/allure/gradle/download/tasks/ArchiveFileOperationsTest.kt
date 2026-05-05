package io.qameta.allure.gradle.download.tasks

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.archivers.zip.UnixStat
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class ArchiveFileOperationsTest {
    @Test
    fun `tar extraction rejects entries with parent traversal`(@TempDir tempDir: Path) {
        val archive = createTarGzipArchive(tempDir, "node.tar.gz") {
            addFile("node-v22.22.0-test/../escape.txt")
        }
        val destination = tempDir.resolve("node")

        assertThatThrownBy {
            ArchiveFileOperations.extractTarGzip(archive.toFile(), destination.toFile())
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Archive entry must not contain '..'")

        assertThat(tempDir.resolve("escape.txt"))
            .doesNotExist()
    }

    @Test
    fun `zip extraction rejects entries with parent traversal`(@TempDir tempDir: Path) {
        val archive = createZipArchive(tempDir, "node.zip") {
            addFile("node-v22.22.0-test/../escape.txt")
        }
        val destination = tempDir.resolve("node")

        assertThatThrownBy {
            ArchiveFileOperations.extractZip(archive.toFile(), destination.toFile())
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Archive entry must not contain '..'")

        assertThat(tempDir.resolve("escape.txt"))
            .doesNotExist()
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun `tar extraction rejects symlinks outside destination`(@TempDir tempDir: Path) {
        val archive = createTarGzipArchive(tempDir, "node.tar.gz") {
            addDirectory("node-v22.22.0-test/bin/")
            addSymlink("node-v22.22.0-test/bin/npm", "../../outside")
        }
        val destination = tempDir.resolve("node")

        assertThatThrownBy {
            ArchiveFileOperations.extractTarGzip(archive.toFile(), destination.toFile())
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Refusing to create symbolic link outside destination")

        assertThat(destination.resolve("bin/npm"))
            .doesNotExist()
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun `zip extraction rejects symlinks outside destination`(@TempDir tempDir: Path) {
        val archive = createZipArchive(tempDir, "node.zip") {
            addDirectory("node-v22.22.0-test/bin/")
            addSymlink("node-v22.22.0-test/bin/npm", "../../outside")
        }
        val destination = tempDir.resolve("node")

        assertThatThrownBy {
            ArchiveFileOperations.extractZip(archive.toFile(), destination.toFile())
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Refusing to create symbolic link outside destination")

        assertThat(destination.resolve("bin/npm"))
            .doesNotExist()
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun `tar extraction rejects entries under symlink parents`(@TempDir tempDir: Path) {
        val archive = createTarGzipArchive(tempDir, "node.tar.gz") {
            addDirectory("node-v22.22.0-test/safe/")
            addSymlink("node-v22.22.0-test/link", "safe")
            addFile("node-v22.22.0-test/link/file.txt")
        }

        assertThatThrownBy {
            ArchiveFileOperations.extractTarGzip(archive.toFile(), tempDir.resolve("node").toFile())
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Archive entry uses symbolic link as parent")
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun `copyDirectory rejects symlinks outside destination`(@TempDir tempDir: Path) {
        val source = tempDir.resolve("source")
        Files.createDirectories(source.resolve("bin"))
        Files.createSymbolicLink(source.resolve("bin/npm"), Path.of("../../outside"))

        assertThatThrownBy {
            ArchiveFileOperations.copyDirectory(source.toFile(), tempDir.resolve("copy").toFile())
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Refusing to create symbolic link outside destination")

        assertThat(tempDir.resolve("copy/bin/npm"))
            .doesNotExist()
    }

    private fun createTarGzipArchive(
        tempDir: Path,
        fileName: String,
        entries: TarArchiveOutputStream.() -> Unit
    ): Path {
        val archive = tempDir.resolve(fileName)
        Files.newOutputStream(archive).use { fileOutput ->
            GZIPOutputStream(fileOutput).use { gzipOutput ->
                TarArchiveOutputStream(gzipOutput).use { tarOutput ->
                    tarOutput.entries()
                }
            }
        }
        return archive
    }

    private fun createZipArchive(
        tempDir: Path,
        fileName: String,
        entries: ZipArchiveOutputStream.() -> Unit
    ): Path {
        val archive = tempDir.resolve(fileName)
        ZipArchiveOutputStream(archive).use { zipOutput ->
            zipOutput.entries()
        }
        return archive
    }

    private fun TarArchiveOutputStream.addDirectory(name: String) {
        val entry = TarArchiveEntry(name)
        entry.mode = TarArchiveEntry.DEFAULT_DIR_MODE
        putArchiveEntry(entry)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.addFile(name: String, content: String = "content\n") {
        val bytes = content.toByteArray(UTF_8)
        val entry = TarArchiveEntry(name)
        entry.mode = TarArchiveEntry.DEFAULT_FILE_MODE
        entry.size = bytes.size.toLong()
        putArchiveEntry(entry)
        write(bytes)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.addSymlink(name: String, target: String) {
        val entry = TarArchiveEntry(name, TarConstants.LF_SYMLINK)
        entry.linkName = target
        putArchiveEntry(entry)
        closeArchiveEntry()
    }

    private fun ZipArchiveOutputStream.addDirectory(name: String) {
        val entry = ZipArchiveEntry(name)
        entry.unixMode = UnixStat.DIR_FLAG or UnixStat.DEFAULT_DIR_PERM
        putArchiveEntry(entry)
        closeArchiveEntry()
    }

    private fun ZipArchiveOutputStream.addFile(name: String, content: String = "content\n") {
        val entry = ZipArchiveEntry(name)
        entry.unixMode = UnixStat.FILE_FLAG or UnixStat.DEFAULT_FILE_PERM
        putArchiveEntry(entry)
        write(content.toByteArray(UTF_8))
        closeArchiveEntry()
    }

    private fun ZipArchiveOutputStream.addSymlink(name: String, target: String) {
        val entry = ZipArchiveEntry(name)
        entry.unixMode = UnixStat.LINK_FLAG or UnixStat.DEFAULT_LINK_PERM
        putArchiveEntry(entry)
        write(target.toByteArray(UTF_8))
        closeArchiveEntry()
    }
}
