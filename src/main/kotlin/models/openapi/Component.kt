package io.github.klahap.fraplin.models.openapi

import kotlinx.serialization.json.JsonElement

interface Component {
    val name: String
    fun toRef(): Ref = Ref(name)
    fun toJson(): Pair<String, JsonElement>

    data class Ref(val name: String) {
        override fun toString() = name
        fun toJson() = jsonOf("#/components/schemas/$name")
    }

    data class Object(
        override val name: String,
        val properties: List<Property>,
    ) : Component {
        override fun toJson() = name to jsonOf(
            "type" to jsonOf("object"),
            "required" to jsonOf(properties.filter { it.required }.map { jsonOf(it.name) }),
            "properties" to jsonOf(properties.associate { it.toJson() }),
        )

        data class Property(
            val name: String,
            val required: Boolean,
            val schema: Schema,
        ) {
            fun toJson() = name to schema.toJson()
        }
    }

    data class StringEnum(
        override val name: String,
        val values: Map<String, String>,
    ) : Component {
        override fun toJson(): Pair<String, JsonElement> {
            val allValues = values.entries.sortedBy { it.key }
            return name to jsonOf(
                "type" to jsonOf("string"),
                "enum" to jsonOf(allValues.map { jsonOf(it.value) }),
                "x-enum-varnames" to jsonOf(allValues.map { jsonOf(it.key) })
            )
        }
    }

    data class IntEnum(
        override val name: String,
        val values: Map<String, Int>,
    ) : Component {
        override fun toJson(): Pair<String, JsonElement> {
            val allValues = values.entries.sortedBy { it.key }
            return name to jsonOf(
                "type" to jsonOf("integer"),
                "enum" to jsonOf(allValues.map { jsonOf(it.value) }),
                "x-enum-varnames" to jsonOf(allValues.map { jsonOf(it.key) })
            )
        }
    }
}