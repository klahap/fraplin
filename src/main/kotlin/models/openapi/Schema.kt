package io.github.klahap.fraplin.models.openapi

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive


sealed interface Schema {
    fun toJson(): JsonElement

    data class Primitive(
        val type: String,
        val nullable: Boolean? = null,
        val format: String? = null,
    ) : Schema {
        override fun toJson() = jsonOf(
            "type" to jsonOf(type),
            format?.let { "format" to jsonOf(it) },
            nullable?.let { "nullable" to jsonOf(it) },
        )
    }

    data class Ref(val value: Component.Ref) : Schema {
        override fun toJson() = jsonOf("\$ref" to value.toJson())
    }

    data class ArrayRef(val value: Component.Ref) : Schema {
        override fun toJson() = jsonOf(
            "type" to JsonPrimitive("array"),
            "items" to jsonOf(
                "\$ref" to value.toJson()
            )
        )
    }
}