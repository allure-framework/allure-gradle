package io.qameta.allure.gradle.download.tasks

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermissions
import java.util.ArrayDeque
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.GZIPInputStream

internal object ArchiveFileOperations {
    private const val POSIX_PERMISSION_SYMBOLS = "rwxrwxrwx"
    private const val POSIX_PERMISSION_BIT_COUNT = 9
    private const val TAR_BLOCK_SIZE = 512
    private const val TAR_NORMAL_FILE = '0'
    private const val TAR_REGULAR_FILE = '\u0000'
    private const val TAR_SYMBOLIC_LINK = '2'
    private const val TAR_DIRECTORY = '5'
    private const val TAR_PAX_EXTENDED_HEADER = 'x'
    private const val TAR_PAX_GLOBAL_HEADER = 'g'
    private const val TAR_GNU_LONG_NAME = 'L'
    private const val TAR_GNU_LONG_LINK = 'K'
    private const val UNIX_FILE_TYPE_MASK = 0b1111_0000_0000_0000
    private const val UNIX_SYMBOLIC_LINK = 0b1010_0000_0000_0000
    private const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50
    private const val CENTRAL_DIRECTORY_HEADER_SIGNATURE = 0x02014b50
    private const val END_OF_CENTRAL_DIRECTORY_MIN_SIZE = 22
    private const val MAX_ZIP_COMMENT_SIZE = 0xffff
    private const val ZIP64_ENTRY_COUNT = 0xffff
    private const val ZIP64_OFFSET_OR_SIZE = 0xffff_ffffL
    private const val ZIP_UTF8_FLAG = 1 shl 11
    private val CP437: Charset = Charset.forName("CP437")

    fun extractTarGzip(archive: File, destinationDir: File) {
        val destination = destinationDir.toPath().normalize().toAbsolutePath()
        Files.createDirectories(destination)

        Files.newInputStream(archive.toPath()).use { fileInput ->
            BufferedInputStream(fileInput).use { bufferedInput ->
                GZIPInputStream(bufferedInput).use { gzipInput ->
                    extractTar(gzipInput, destination)
                }
            }
        }
    }

    fun extractZip(archive: File, destinationDir: File) {
        val destination = destinationDir.toPath().normalize().toAbsolutePath()
        Files.createDirectories(destination)

        val unixModesByName = readZipUnixModes(archive)
        ZipFile(archive, UTF_8).use { zipFile ->
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val unixMode = unixModesByName[entry.name]?.pollFirst() ?: 0
                extractZipEntry(zipFile, entry, unixMode, destination)
            }
        }
    }

    fun copyDirectory(sourceDir: File, destinationDir: File) {
        val source = sourceDir.toPath().normalize().toAbsolutePath()
        val destination = destinationDir.toPath().normalize().toAbsolutePath()
        validateCopiedSymlinks(source, destination)
        Files.createDirectories(destination.parent)
        Files.walkFileTree(
            source,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val target = resolveChild(destination, source.relativize(dir))
                    ensureNoSymlinkParent(destination, target)
                    Files.createDirectories(target)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (exc != null) {
                        throw exc
                    }
                    copyDirectoryAttributes(dir, resolveChild(destination, source.relativize(dir)))
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val target = resolveChild(destination, source.relativize(file))
                    ensureNoSymlinkParent(destination, target)
                    Files.createDirectories(target.parent)
                    if (attrs.isSymbolicLink) {
                        createSymlink(destination, target, Files.readSymbolicLink(file).toString())
                    } else {
                        Files.copy(
                            file,
                            target,
                            LinkOption.NOFOLLOW_LINKS,
                            StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }
                    return FileVisitResult.CONTINUE
                }
            }
        )
    }

    private fun extractZipEntry(zipFile: ZipFile, entry: ZipEntry, unixMode: Int, destination: Path) {
        val target = resolveArchiveEntry(destination, entry.name) ?: return
        ensureNoSymlinkParent(destination, target)
        when {
            entry.isDirectory -> {
                Files.createDirectories(target)
                applyPosixPermissions(target, unixMode)
            }
            isUnixSymlink(unixMode) -> {
                Files.createDirectories(target.parent)
                zipFile.getInputStream(entry).use { input ->
                    createSymlink(destination, target, input.readBytes().toString(UTF_8))
                }
            }
            else -> {
                Files.createDirectories(target.parent)
                zipFile.getInputStream(entry).use { input ->
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
                }
                applyPosixPermissions(target, unixMode)
            }
        }
    }

    private fun extractTarEntry(tarInput: InputStream, entry: TarEntry, destination: Path) {
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

    private fun extractTar(input: InputStream, destination: Path) {
        val globalPaxHeaders = linkedMapOf<String, String>()
        var paxHeaders = emptyMap<String, String>()
        var longName: String? = null
        var longLinkName: String? = null

        while (true) {
            val header = readTarHeader(input) ?: return
            val size = parseTarNumber(header, 124, 12)
            val typeFlag = header[156].toInt().toChar()

            when (typeFlag) {
                TAR_PAX_EXTENDED_HEADER -> {
                    paxHeaders = parsePaxHeaders(readTarEntryBytes(input, size))
                    skipTarPadding(input, size)
                    continue
                }
                TAR_PAX_GLOBAL_HEADER -> {
                    globalPaxHeaders.putAll(parsePaxHeaders(readTarEntryBytes(input, size)))
                    skipTarPadding(input, size)
                    continue
                }
                TAR_GNU_LONG_NAME -> {
                    longName = readTarEntryBytes(input, size).toString(UTF_8).trimEnd('\u0000')
                    skipTarPadding(input, size)
                    continue
                }
                TAR_GNU_LONG_LINK -> {
                    longLinkName = readTarEntryBytes(input, size).toString(UTF_8).trimEnd('\u0000')
                    skipTarPadding(input, size)
                    continue
                }
            }

            val mergedPaxHeaders = globalPaxHeaders + paxHeaders
            val entry = TarEntry(
                name = mergedPaxHeaders["path"] ?: longName ?: parseTarName(header),
                mode = parseTarNumber(header, 100, 8).toInt(),
                size = size,
                typeFlag = typeFlag,
                linkName = mergedPaxHeaders["linkpath"] ?: longLinkName ?: parseTarString(header, 157, 100)
            )
            paxHeaders = emptyMap()
            longName = null
            longLinkName = null

            val entryInput = BoundedInputStream(input, size)
            if (entry.isFile || entry.isDirectory || entry.isSymbolicLink) {
                extractTarEntry(entryInput, entry, destination)
            }
            entryInput.drain()
            skipTarPadding(input, size)
        }
    }

    private fun readTarHeader(input: InputStream): ByteArray? {
        val header = ByteArray(TAR_BLOCK_SIZE)
        var offset = 0
        while (offset < header.size) {
            val read = input.read(header, offset, header.size - offset)
            if (read == -1) {
                if (offset == 0) {
                    return null
                }
                throw EOFException("Unexpected end of TAR header")
            }
            offset += read
        }
        return if (header.all { it == 0.toByte() }) {
            null
        } else {
            header
        }
    }

    private fun readTarEntryBytes(input: InputStream, size: Long): ByteArray {
        require(size <= Int.MAX_VALUE) {
            "TAR metadata entry is too large: $size bytes"
        }
        val bytes = ByteArray(size.toInt())
        var offset = 0
        while (offset < bytes.size) {
            val read = input.read(bytes, offset, bytes.size - offset)
            if (read == -1) {
                throw EOFException("Unexpected end of TAR entry")
            }
            offset += read
        }
        return bytes
    }

    private fun skipTarPadding(input: InputStream, size: Long) {
        val padding = (TAR_BLOCK_SIZE - size % TAR_BLOCK_SIZE) % TAR_BLOCK_SIZE
        skipBytes(input, padding)
    }

    private fun skipBytes(input: InputStream, bytes: Long) {
        var remaining = bytes
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read == -1) {
                throw EOFException("Unexpected end of archive")
            }
            remaining -= read
        }
    }

    private fun parseTarName(header: ByteArray): String {
        val name = parseTarString(header, 0, 100)
        val prefix = parseTarString(header, 345, 155)
        return if (prefix.isBlank()) name else "$prefix/$name"
    }

    private fun parseTarString(header: ByteArray, offset: Int, length: Int): String {
        var end = offset
        val maxEnd = offset + length
        while (end < maxEnd && header[end] != 0.toByte()) {
            end += 1
        }
        return header.copyOfRange(offset, end).toString(UTF_8).trim()
    }

    private fun parseTarNumber(header: ByteArray, offset: Int, length: Int): Long {
        val first = header[offset].toInt() and 0xff
        if (first and 0x80 != 0) {
            var result = (first and 0x7f).toLong()
            for (index in offset + 1 until offset + length) {
                result = (result shl 8) or (header[index].toInt() and 0xff).toLong()
            }
            return result
        }

        var result = 0L
        var index = offset
        val end = offset + length
        while (index < end && (header[index] == 0.toByte() || header[index] == ' '.code.toByte())) {
            index += 1
        }
        while (index < end) {
            val value = header[index].toInt()
            if (value !in '0'.code..'7'.code) {
                break
            }
            result = (result shl 3) + (value - '0'.code)
            index += 1
        }
        return result
    }

    private fun parsePaxHeaders(bytes: ByteArray): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var index = 0
        while (index < bytes.size) {
            var lengthEnd = index
            while (lengthEnd < bytes.size && bytes[lengthEnd] != ' '.code.toByte()) {
                lengthEnd += 1
            }
            require(lengthEnd < bytes.size) {
                "Invalid PAX header record"
            }
            val length = bytes.copyOfRange(index, lengthEnd).toString(UTF_8).toInt()
            require(length > 0 && index + length <= bytes.size) {
                "Invalid PAX header length: $length"
            }
            val recordStart = lengthEnd + 1
            val recordEnd = index + length - 1
            val record = bytes.copyOfRange(recordStart, recordEnd).toString(UTF_8)
            val separator = record.indexOf('=')
            if (separator > 0) {
                result[record.substring(0, separator)] = record.substring(separator + 1)
            }
            index += length
        }
        return result
    }

    private fun readZipUnixModes(archive: File): Map<String, ArrayDeque<Int>> =
        RandomAccessFile(archive, "r").use { file ->
            val endOfCentralDirectory = findEndOfCentralDirectory(file)
            file.seek(endOfCentralDirectory + 10)
            val totalEntries = file.readUnsignedShortLE()
            val centralDirectorySize = file.readUnsignedIntLE()
            val centralDirectoryOffset = file.readUnsignedIntLE()
            require(totalEntries != ZIP64_ENTRY_COUNT && centralDirectorySize != ZIP64_OFFSET_OR_SIZE &&
                centralDirectoryOffset != ZIP64_OFFSET_OR_SIZE) {
                "Zip64 archives are not supported"
            }

            file.seek(centralDirectoryOffset)
            val modesByName = linkedMapOf<String, ArrayDeque<Int>>()
            repeat(totalEntries) {
                val signature = file.readIntLE()
                require(signature == CENTRAL_DIRECTORY_HEADER_SIGNATURE) {
                    "Invalid ZIP central directory header"
                }
                file.skipFully(4)
                val flags = file.readUnsignedShortLE()
                file.skipFully(18)
                val nameLength = file.readUnsignedShortLE()
                val extraLength = file.readUnsignedShortLE()
                val commentLength = file.readUnsignedShortLE()
                file.skipFully(4)
                val externalAttributes = file.readUnsignedIntLE()
                file.skipFully(4)

                val nameBytes = ByteArray(nameLength)
                file.readFully(nameBytes)
                file.skipFully(extraLength + commentLength)

                val nameCharset = if (flags and ZIP_UTF8_FLAG != 0) UTF_8 else CP437
                val name = nameBytes.toString(nameCharset)
                val unixMode = (externalAttributes ushr 16).toInt()
                modesByName.getOrPut(name) { ArrayDeque() }.add(unixMode)
            }
            modesByName
        }

    private fun findEndOfCentralDirectory(file: RandomAccessFile): Long {
        val fileLength = file.length()
        val searchLength = minOf(fileLength, END_OF_CENTRAL_DIRECTORY_MIN_SIZE + MAX_ZIP_COMMENT_SIZE.toLong())
            .toInt()
        val buffer = ByteArray(searchLength)
        file.seek(fileLength - searchLength)
        file.readFully(buffer)

        for (index in buffer.size - END_OF_CENTRAL_DIRECTORY_MIN_SIZE downTo 0) {
            if (buffer.readIntLE(index) == END_OF_CENTRAL_DIRECTORY_SIGNATURE) {
                val commentLength = buffer.readUnsignedShortLE(index + 20)
                if (index + END_OF_CENTRAL_DIRECTORY_MIN_SIZE + commentLength == buffer.size) {
                    return fileLength - searchLength + index
                }
            }
        }
        throw IllegalArgumentException("Invalid ZIP archive: missing end of central directory")
    }

    private fun RandomAccessFile.readUnsignedShortLE(): Int {
        val byte0 = read()
        val byte1 = read()
        if (byte0 == -1 || byte1 == -1) {
            throw EOFException("Unexpected end of ZIP archive")
        }
        return byte0 or (byte1 shl 8)
    }

    private fun RandomAccessFile.readUnsignedIntLE(): Long {
        val byte0 = read()
        val byte1 = read()
        val byte2 = read()
        val byte3 = read()
        if (byte0 == -1 || byte1 == -1 || byte2 == -1 || byte3 == -1) {
            throw EOFException("Unexpected end of ZIP archive")
        }
        return (byte0.toLong() or
            (byte1.toLong() shl 8) or
            (byte2.toLong() shl 16) or
            (byte3.toLong() shl 24)) and ZIP64_OFFSET_OR_SIZE
    }

    private fun RandomAccessFile.readIntLE(): Int =
        readUnsignedIntLE().toInt()

    private fun RandomAccessFile.skipFully(bytes: Int) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = skipBytes(remaining)
            if (skipped <= 0) {
                throw EOFException("Unexpected end of ZIP archive")
            }
            remaining -= skipped
        }
    }

    private fun ByteArray.readIntLE(offset: Int): Int =
        (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            ((this[offset + 3].toInt() and 0xff) shl 24)

    private fun ByteArray.readUnsignedShortLE(offset: Int): Int =
        (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)

    private fun copyDirectoryAttributes(source: Path, target: Path) {
        runCatching {
            Files.setLastModifiedTime(target, Files.getLastModifiedTime(source, LinkOption.NOFOLLOW_LINKS))
        }
        runCatching {
            Files.setPosixFilePermissions(
                target,
                Files.getPosixFilePermissions(source, LinkOption.NOFOLLOW_LINKS)
            )
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
        if (target == destination) {
            return
        }
        val parent = target.parent ?: return
        if (!parent.startsWith(destination)) {
            return
        }
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

    private fun isUnixSymlink(unixMode: Int) =
        unixMode and UNIX_FILE_TYPE_MASK == UNIX_SYMBOLIC_LINK

    private data class TarEntry(
        val name: String,
        val mode: Int,
        val size: Long,
        val typeFlag: Char,
        val linkName: String
    ) {
        val isDirectory: Boolean
            get() = typeFlag == TAR_DIRECTORY || name.endsWith("/")

        val isSymbolicLink: Boolean
            get() = typeFlag == TAR_SYMBOLIC_LINK

        val isFile: Boolean
            get() = typeFlag == TAR_NORMAL_FILE || typeFlag == TAR_REGULAR_FILE
    }

    private class BoundedInputStream(
        private val input: InputStream,
        private var remaining: Long
    ) : InputStream() {
        override fun read(): Int {
            if (remaining == 0L) {
                return -1
            }
            val byte = input.read()
            if (byte != -1) {
                remaining -= 1
            }
            return byte
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining == 0L) {
                return -1
            }
            val read = input.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
            if (read > 0) {
                remaining -= read.toLong()
            }
            return read
        }

        fun drain() {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (read(buffer) != -1) {
                // Keep reading until this entry's payload is consumed.
            }
        }
    }
}
