package io.github.klahap.fraplin.util

import com.squareup.kotlinpoet.FileSpec
import java.nio.file.Path
import kotlin.io.path.*

class DirectorySynchronizer(
    private val directory: Path
) {
    private var filesCreated = mutableSetOf<Path>()
    private var filesUpdated = mutableSetOf<Path>()
    private var filesUnchanged = mutableSetOf<Path>()

    private fun checkFilePath(path: Path) {
        val inDirectory = null != path.absolute().relativeToOrNull(directory.absolute())
        if (!inDirectory) throw Exception("path '$path' is not in output directory '$directory'")
    }

    fun sync(relativePath: String, content: String) = sync(
        path = directory.resolve(relativePath).absolute(),
        content = content,
    )

    fun sync(
        packageName: String,
        relativePath: String,
        block: FileSpec.Builder.() -> Unit,
    ) = FileSpec.builder(packageName, relativePath.split("/").last()).apply(block).build().let {
        sync(relativePath = relativePath, content = it)
    }

    fun sync(
        relativePath: String,
        content: FileSpec,
    ) = sync(relativePath = relativePath, content = content.toString())

    private fun sync(path: Path, content: String) {
        checkFilePath(path)
        val type = DirectorySynchronizer.sync(path = path, content = content)
        when (type) {
            FileSyncType.UNCHANGED -> filesUnchanged.add(path)
            FileSyncType.UPDATED -> filesUpdated.add(path)
            FileSyncType.CREATED -> filesCreated.add(path)
        }
    }

    fun cleanup() {
        @OptIn(ExperimentalPathApi::class)
        val actualFiles = directory.walk().map { it.absolute() }.toSet()
        val filesToDelete = actualFiles - (filesCreated + filesUpdated + filesUnchanged)
        filesToDelete.forEach { it.deleteExisting() }

        fun Set<*>.printSize() = size.toString().padStart(3)
        println("#files unchanged = ${filesUnchanged.printSize()}")
        println("#files created   = ${filesCreated.printSize()}")
        println("#files updated   = ${filesUpdated.printSize()}")
        println("#files deleted   = ${filesToDelete.printSize()}")
    }

    enum class FileSyncType {
        UNCHANGED, UPDATED, CREATED
    }

    companion object {
        fun sync(
            path: Path,
            content: String,
        ): FileSyncType {
            if (!path.isAbsolute) return sync(path = path.absolute(), content = content)
            return if (path.exists()) {
                if (path.readText() == content) {
                    FileSyncType.UNCHANGED
                } else {
                    path.writeText(content)
                    FileSyncType.UPDATED
                }
            } else {
                path.parent.createDirectories()
                path.writeText(content)
                FileSyncType.CREATED
            }
        }
    }
}