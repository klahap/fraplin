package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.*
import io.github.klahap.fraplin.models.DocTypeDataRaw.Companion.sum
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

    suspend fun getAll(repos: Set<GitRepo>, docTypeInfos: Set<DocTypeInfo>): Collection<DocType.Full> =
        repos.map { getAll(it) }.sum().merge(additionalInfo = docTypeInfos)

    private suspend fun getAll(repo: GitRepo): DocTypeDataRaw = withContext(Dispatchers.IO) {
        when (repo) {
            is GitRepo.Local -> getAll(appDir = repo.path.resolve(repo.appName))
            is GitRepo.GitHub -> tempDir { dir ->
                try {
                    runProcess(cwd = dir, "git", "init")
                    runProcess(cwd = dir, "git", "remote", "add", "origin", repo.cloneUrl)
                    runProcess(cwd = dir, "git", "fetch", "--depth", "1", "origin", repo.version ?: "HEAD")
                    runProcess(cwd = dir, "git", "checkout", "FETCH_HEAD")
                } catch (e: Exception) {
                    throw Exception("cannot clone GitHub repo '${repo.url}', msg=${e.message}")
                }
                getAll(appDir = dir.resolve(repo.appName))
            }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun getAll(appDir: Path): DocTypeDataRaw =
        withContext(Dispatchers.IO) {
            val fileMatcher = Regex("^[^/]+/doctype/([^/]+)/\\1\\.json$")
            val paths = appDir.walk().filter {
                val relativePath = it.relativeTo(appDir).toString()
                fileMatcher.matches(relativePath)
            }.toSet()
            val data = paths.map { jsonPath ->
                val data = json.decodeFromString<JsonObject>(jsonPath.readText())
                val docTypeRaw = json.decodeFromJsonElement<DocTypeRaw>(data)
                val docFieldsRaw = json.decodeFromJsonElement<Collection<DocFieldJsonRaw>>(data["fields"]!!)
                    .map { it.toDocTypeRaw(docTypeRaw.name) }
                DocTypeDataRaw(
                    docTypes = listOf(docTypeRaw),
                    docFields = docFieldsRaw,
                )
            }.sum()
            val customFields = appDir.resolve("fixtures/custom_field.json")
                .takeIf { it.exists() }?.readText()
                ?.let { json.decodeFromString<Collection<DocFieldRaw.Custom>>(it) }
                ?: emptyList()
            data + DocTypeDataRaw(docFields = customFields)
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