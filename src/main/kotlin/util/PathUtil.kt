package io.github.klahap.fraplin.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively


data class DefaultCodeFile(
    private val relativePackageNames: List<String>,
    private val fileName: String,
) {
    val relativePath get() = (relativePackageNames + listOf(fileName)).joinToString("/")

    suspend fun getContent(packageName: String): String = withContext(Dispatchers.IO) {
        javaClass.getResourceAsStream("/default_code/$relativePath")!!
            .readAllBytes()
    }.decodeToString()
        .replaceFirst("package default_code", "package $packageName")
        .replace("import default_code", "import $packageName")

    companion object {
        private fun getResources(path: List<String>): Sequence<List<String>> {
            val pathStr = path.joinToString("/")
            val resource = javaClass.classLoader.getResource(pathStr) ?: error("Resource not found: $pathStr")
            val childs = File(resource.toURI()).list()?.toList() ?: return sequenceOf(path)
            return childs.asSequence().flatMap { child ->
                getResources(path + listOf(child))
            }
        }

        fun all() = getResources(listOf("default_code"))
            .map { DefaultCodeFile(it.drop(1).dropLast(1), it.last()) }.toSet()
    }
}


object PathUtil {
    suspend fun <T> tempDir(block: suspend (Path) -> T): T = createTempDirectory().let {
        val result = runCatching { block(it) }
        @OptIn(ExperimentalPathApi::class) it.deleteRecursively()
        result.getOrThrow()
    }
}
