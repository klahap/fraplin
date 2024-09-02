package io.github.klahap.fraplin.models

import io.github.klahap.fraplin.util.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class FraplinOpenApiSpec(
    val name: String,
    val version: String,
    val pathPrefix: String,
    val schemaPrefix: String,
    val pathTags: Set<String>,
    val docTypes: List<DocType.Virtual>,
)
