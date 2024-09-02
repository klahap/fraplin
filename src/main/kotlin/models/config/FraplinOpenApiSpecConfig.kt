package io.github.klahap.fraplin.models.config

import io.github.klahap.fraplin.models.VirtualDocTypeInfo
import kotlinx.serialization.Serializable

@Serializable
data class FraplinOpenApiSpecConfig(
    val name: String,
    val version: String,
    val pathPrefix: String,
    val schemaPrefix: String,
    val pathTags: Set<String>,
    val docTypes: Set<VirtualDocTypeInfo>,
) {
    class Builder(
        var name: String,
        var version: String = "1.0.0",
        val pathTags: MutableSet<String> = mutableSetOf(),
        var pathPrefix: String = "/frappe/",
        var schemaPrefix: String = "Frappe",
        private var virtualDocTypes: MutableSet<VirtualDocTypeInfo> = mutableSetOf(),
    ) {
        fun addDocType(
            name: String,
            strictTyped: Boolean = false,
            block: VirtualDocTypeInfo.Builder.() -> Unit = {},
        ) {
            VirtualDocTypeInfo.Builder(name = name, strictTyped = strictTyped)
                .apply(block).build()
                .also { virtualDocTypes.add(it) }
        }

        fun build() = FraplinOpenApiSpecConfig(
            name = name,
            version = version,
            pathTags = pathTags.toSet(),
            schemaPrefix = schemaPrefix,
            pathPrefix = pathPrefix,
            docTypes = virtualDocTypes.toSortedSet()
                .takeIf { it.isNotEmpty() }
                ?: throw Exception("no doc types defined"),
        )
    }
}
