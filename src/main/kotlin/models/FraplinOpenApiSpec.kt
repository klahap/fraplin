package io.github.klahap.fraplin.models

import kotlinx.serialization.Serializable

@Serializable
data class FraplinOpenApiSpec(
    val name: String,
    val title: String,
    val version: String,
    val pathPrefix: String,
    val schemaPrefix: String,
    val docStatusAsInteger: Boolean,
    val pathTags: Set<String>,
    val docTypes: List<DocType.Virtual>,
)
