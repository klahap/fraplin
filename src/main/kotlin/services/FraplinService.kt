package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.FraplinSpec
import io.github.klahap.fraplin.models.config.FraplinConfig
import io.github.klahap.fraplin.models.config.FraplinInputConfig
import io.github.klahap.fraplin.models.config.FraplinOutputConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FraplinService(
    client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { prettyPrint = true },
) {
    private val specService: FraplinSpecService = FraplinSpecService(client)
    private val codeGenService: FrappeCodeGenService = FrappeCodeGenService()

    suspend fun readConfig(path: Path): FraplinConfig = json.decodeFromString(readText(path))
    suspend fun readSpec(path: Path): FraplinSpec = json.decodeFromString(readText(path))

    suspend fun generateSpec(config: FraplinInputConfig) = specService.getSpec(config)
    suspend fun generateDsl(config: FraplinOutputConfig, spec: FraplinSpec) =
        codeGenService.generate(config = config, spec = spec)

    suspend fun writeSpec(spec: FraplinSpec, path: Path) = withContext(Dispatchers.IO) {
        path.writeText(json.encodeToString(spec))
    }

    companion object {
        private suspend fun readText(path: Path): String = withContext(Dispatchers.IO) {
            if (!path.exists()) throw FileNotFoundException("file $path not found")
            path.readText()
        }
    }
}