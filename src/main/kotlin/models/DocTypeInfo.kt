package io.github.klahap.fraplin.models

import kotlinx.serialization.Serializable

@Serializable
data class DocTypeInfo(
    val name: String,
    val strictTyped: Boolean = false,
) {
    val docTypeName get() = DocType.Name(name)

    init {
        assert(name.isNotBlank())
    }
}
