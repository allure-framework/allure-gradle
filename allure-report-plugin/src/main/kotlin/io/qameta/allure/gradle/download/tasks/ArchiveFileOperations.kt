package io.qameta.allure.gradle.download.tasks

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.file.PathUtils
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermissions
import java.util.zip.GZIPInputStream

internal object ArchiveFileOperations {
    private const val POSIX_PERMISSION_SYMBOLS = "rwxrwxrwx"
    private const val POSIX_PERMISSION_BIT_COUNT = 9

    fun extractTarGzip(archive: File, destinationDir: File) {
        val destination = destinationDir.toPath().normalize().toAbsolutePath()
        Files.createDirectories(destination)

        Files.newInputStream(archive.toPath()).use { fileInput ->
            BufferedInputStream(fileInput).use { bufferedInput ->
                GZIPInputStream(bufferedInput).use { gzipInput ->
                    TarArchiveInputStream(gzipInput).use { tarInput ->
                        var entry = tarInput.nextEntry
                        while (entry != null) {
                            extractEntry(tarInput, entry, destination)
                            entry = tarInput.nextEntry
                        }
                    }
                }
            }
        }
    }

    fun extractZip(archive: File, destinationDir: File) {
        val destination = destinationDir.toPath().normalize().toAbsolutePath()
        Files.createDirectories(destination)

        ZipFile.builder().setFile(archive).get().use { zipFile ->
            val entries = zipFile.entries
            while (entries.hasMoreElements()) {
                extractZipEntry(zipFile, entries.nextElement(), destination)
            }
        }
    }

    fun copyDirectory(sourceDir: File, destinationDir: File) {
        val source = sourceDir.toPath().normalize().toAbsolutePath()
        val destination = destinationDir.toPath().normalize().toAbsolutePath()
        validateCopiedSymlinks(source, destination)
        Files.createDirectories(destination.parent)
        PathUtils.copyDirectory(
            source,
            destination,
            LinkOption.NOFOLLOW_LINKS,
            StandardCopyOption.COPY_ATTRIBUTES,
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    private fun extractZipEntry(zipFile: ZipFile, entry: ZipArchiveEntry, destination: Path) {
        val target = resolveArchiveEntry(destination, entry.name) ?: return
        ensureNoSymlinkParent(destination, target)
        when {
            entry.isDirectory -> {
                Files.createDirectories(target)
                applyPosixPermissions(target, entry.unixMode)
            }
            entry.isUnixSymlink -> {
                Files.createDirectories(target.parent)
                createSymlink(destination, target, zipFile.getUnixSymlink(entry))
            }
            else -> {
                Files.createDirectories(target.parent)
                zipFile.getInputStream(entry).use { input ->
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
                }
                applyPosixPermissions(target, entry.unixMode)
            }
        }
    }

    private fun extractEntry(tarInput: TarArchiveInputStream, entry: TarArchiveEntry, destination: Path) {
        val target = resolveArchiveEntry(destination, entry.name) ?: return
        ensureNoSymlinkParent(destination, target)
        when {
            entry.isDirectory -> {
                Files.createDirectories(target)
                applyPosixPermissions(target, entry.mode)
            }
            entry.isSymbolicLink -> {
                Files.createDirectories(target.parent)
                createSymlink(destination, target, entry.linkName)
            }
            entry.isFile -> {
                Files.createDirectories(target.parent)
                Files.copy(tarInput, target, StandardCopyOption.REPLACE_EXISTING)
                applyPosixPermissions(target, entry.mode)
            }
        }
    }

    private fun createSymlink(destination: Path, target: Path, linkName: String) {
        val linkTarget = Path.of(linkName)
        validateSymlinkTarget(destination, target, linkName)
        Files.deleteIfExists(target)
        Files.createSymbolicLink(target, linkTarget)
    }

    private fun validateCopiedSymlinks(source: Path, destination: Path) {
        Files.walkFileTree(
            source,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (Files.isSymbolicLink(file)) {
                        val target = resolveChild(destination, source.relativize(file))
                        validateSymlinkTarget(destination, target, Files.readSymbolicLink(file).toString())
                    }
                    return FileVisitResult.CONTINUE
                }
            }
        )
    }

    private fun validateSymlinkTarget(destination: Path, target: Path, linkName: String) {
        val linkTarget = Path.of(linkName)
        require(!linkTarget.isAbsolute) {
            "Refusing to create absolute symbolic link $target -> $linkName"
        }
        val resolvedLinkTarget = target.parent.resolve(linkTarget).normalize()
        require(resolvedLinkTarget.startsWith(destination)) {
            "Refusing to create symbolic link outside destination: $target -> $linkName"
        }
    }

    private fun resolveArchiveEntry(destination: Path, entryName: String): Path? {
        val normalizedName = entryName.replace('\\', '/')
        require(!normalizedName.startsWith("/")) {
            "Archive entry must be relative: $entryName"
        }
        val segments = normalizedName.split("/")
            .filter { it.isNotEmpty() && it != "." }
        require(segments.none { it == ".." }) {
            "Archive entry must not contain '..': $entryName"
        }
        if (segments.size <= 1) {
            return null
        }
        return resolveChild(destination, segments.drop(1).fold(Path.of("")) { path, segment -> path.resolve(segment) })
    }

    private fun resolveChild(destination: Path, relativePath: Path): Path {
        val target = destination.resolve(relativePath).normalize()
        require(target.startsWith(destination)) {
            "Archive entry escapes destination: $relativePath"
        }
        return target
    }

    private fun ensureNoSymlinkParent(destination: Path, target: Path) {
        val parent = target.parent ?: return
        var current = destination
        for (segment in destination.relativize(parent)) {
            current = current.resolve(segment)
            require(!Files.isSymbolicLink(current)) {
                "Archive entry uses symbolic link as parent: $target"
            }
        }
    }

    private fun applyPosixPermissions(path: Path, mode: Int) {
        if (mode == 0) {
            return
        }
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(posixPermissionString(mode)))
        } catch (_: UnsupportedOperationException) {
            path.toFile().setExecutable(posixPermissionString(mode).hasAnyExecutePermission(), false)
        }
    }

    private fun posixPermissionString(mode: Int) = buildString(POSIX_PERMISSION_BIT_COUNT) {
        POSIX_PERMISSION_SYMBOLS.forEachIndexed { index, symbol ->
            val bit = 1 shl (POSIX_PERMISSION_BIT_COUNT - index - 1)
            append(if (mode and bit != 0) symbol else '-')
        }
    }

    private fun String.hasAnyExecutePermission() =
        filterIndexed { index, _ -> index % 3 == 2 }.any { it == 'x' }
}
