package io.github.klahap.fraplin.models

data class OpenApiGenContext(
    val pathPrefix: String,
    val schemaPrefix: String,
    val tags: Set<String>,
    val docStatusAsInteger: Boolean,
)
