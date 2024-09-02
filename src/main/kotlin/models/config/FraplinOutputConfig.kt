package io.github.klahap.fraplin.models.config

import io.github.klahap.fraplin.util.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.absolute


@Serializable
data class FraplinOutputConfig(
    @Serializable(PathSerializer::class) val path: Path,
    val packageName: String,
    @Serializable(PathSerializer::class) val openApiPath: Path?,
) {
    data class Builder(
        var path: Path? = null,
        var packageName: String? = null,
        var openApiPath: Path? = null,
    ) {
        fun build() = FraplinOutputConfig(
            path = path?.absolute() ?: throw Exception("no output path defined"),
            packageName = packageName ?: throw Exception("no output package name defined"),
            openApiPath = openApiPath,
        )
    }
}

