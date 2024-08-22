package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.*
import io.github.klahap.fraplin.models.FrappeSchema.Collector.Companion.sum
import io.github.klahap.fraplin.models.config.GitRepo
import io.github.klahap.fraplin.util.BooleanAsIntSerializer
import io.github.klahap.fraplin.util.PathUtil.tempDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.nio.file.Path
import kotlin.io.path.*

class FrappeGitRepoService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAll(repos: Set<GitRepo>, docTypeInfos: Set<DocTypeInfo>): FrappeSchema =
        repos.map { repo -> useRepo(repo) { getAll(repo = this) } }.sum().collect(additionalInfo = docTypeInfos)

    private suspend fun <T> useRepo(repo: GitRepo, block: suspend GitRepo.Local.() -> T): T = when (repo) {
        is GitRepo.Local -> repo.block()
        is GitRepo.GitHub -> tempDir { dir ->
            try {
                runProcess(cwd = dir, "git", "init")
                runProcess(cwd = dir, "git", "remote", "add", "origin", repo.cloneUrl)
                runProcess(cwd = dir, "git", "fetch", "--depth", "1", "origin", repo.version ?: "HEAD")
                runProcess(cwd = dir, "git", "checkout", "FETCH_HEAD")
            } catch (e: Exception) {
                throw Exception("cannot clone GitHub repo '${repo.url}', msg=${e.message}")
            }
            GitRepo.Local(path = dir, appName = repo.appName).block()
        }
    }

    private fun findAllWhitelistedFunction(repo: GitRepo.Local, filePath: Path): Set<WhiteListFunction> {
        val prefix = filePath.relativeTo(repo.path).map { it.toString() }.let { parts ->
            if (parts.isEmpty()) return@let parts
            val fileBaseName = parts.last().removeSuffix(".py")
            parts.dropLast(1).let {
                if (fileBaseName == "__init__") it
                else it + listOf(fileBaseName)
            }
        }.joinToString(".")
        return whiteListedRegex.findAll(filePath.readText()).map { match ->
            val name = match.groups["name"]!!.value
            val options = match.groups["options"]!!.value
            val args = match.groups["args"]!!.value
                .replace(Regex("\\[.*]"), "")
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { raw ->
                    val parts = raw.trim().split(':').map { it.trim() }
                    if (parts.first().startsWith("*")) return@mapNotNull null
                    when (parts.size) {
                        1 -> parts.single() to null
                        2 -> parts.first() to parts.last()
                        else -> throw Exception("unexpected function argument: '$raw'")
                    }
                }
                .map {
                    WhiteListFunction.Arg(
                        name = it.first,
                        type = WhiteListFunction.Arg.Type.fromPythonType(it.second)
                    )
                }
            val isPublic = whiteListedIsPublicRegex.find(options)?.groups?.get("value")?.value?.let {
                when (it) {
                    "False" -> false
                    "True" -> true
                    else -> throw Exception("unexpected value for allow_guest='$it'")
                }
            } ?: false
            WhiteListFunction(
                name = "$prefix.$name",
                args = args,
                isPublic = isPublic
            )
        }.toSet()
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun getAll(repo: GitRepo.Local): FrappeSchema.Collector =
        withContext(Dispatchers.IO) {
            val fileMatcher = Regex("^[^/]+/doctype/([^/]+)/\\1\\.json$")
            val docTypeJsonPaths = repo.appPath.walk().filter {
                val relativePath = it.relativeTo(repo.appPath).toString()
                fileMatcher.matches(relativePath)
            }.toSet()
            val whitelistedFunctions = if (repo.addWhiteListedFunctions)
                repo.appPath.walk()
                    .filter { it.extension == "py" }
                    .flatMap { findAllWhitelistedFunction(repo, it) }
                    .toSet()
            else
                emptySet()
            val docTypes = docTypeJsonPaths.map { jsonPath ->
                val data = json.decodeFromString<JsonObject>(jsonPath.readText())
                val docTypeRaw = json.decodeFromJsonElement<DocTypeRaw>(data)
                val docFieldsRaw = json.decodeFromJsonElement<Collection<DocFieldJsonRaw>>(data["fields"]!!)
                    .map { it.toDocTypeRaw(docTypeRaw.name) }
                FrappeSchema.Collector(
                    docTypes = listOf(docTypeRaw),
                    docFields = docFieldsRaw,
                )
            }.sum()
            val customFields = repo.appPath.resolve("fixtures/custom_field.json")
                .takeIf { it.exists() }?.readText()
                ?.let { json.decodeFromString<Collection<DocFieldRaw.Custom>>(it) }
                ?: emptyList()
            docTypes + FrappeSchema.Collector(
                docFields = customFields,
                whiteListFunctions = whitelistedFunctions,
            )
        }

    @Serializable
    private data class DocFieldJsonRaw(
        @SerialName("fieldname") val fieldName: String,
        @SerialName("fieldtype") val fieldType: FieldTypeRaw,
        @Serializable(with = BooleanAsIntSerializer::class)
        @SerialName("reqd") val required: Boolean = false,
        @SerialName("options") val options: String? = null,
    ) {
        fun toDocTypeRaw(parent: DocType.Name) = DocFieldRaw.Common(
            parent = parent,
            fieldName = fieldName,
            fieldType = fieldType,
            required = required,
            options = options,
        )
    }

    companion object {
        private val whiteListedRegex =
            Regex("\\s*@(?:frappe\\.)?whitelist\\((?<options>.*)\\)\\s*\\n\\s*def\\s+(?<name>\\w+)\\((?<args>[^)]*)\\)")
        private val whiteListedIsPublicRegex =
            Regex("allow_guest\\s*=\\s*(?<value>\\w+)")

        private fun runProcess(cwd: Path, vararg cmd: String) {
            val process = ProcessBuilder().apply {
                directory(cwd.absolute().toFile())
                command(cmd.toList())
            }.start()!!
            if (process.waitFor() != 0) {
                val msg = process.errorReader().readText()
                throw Exception(msg)
            }
        }
    }
}