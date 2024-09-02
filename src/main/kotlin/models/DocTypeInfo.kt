package io.github.klahap.fraplin.models

import kotlinx.serialization.Serializable

@Serializable
data class DocTypeInfo(
    val name: String,
    val strictTyped: Boolean = false,
) : Comparable<DocTypeInfo> {
    val docTypeName get() = DocType.Name(name)

    override fun compareTo(other: DocTypeInfo) = name.compareTo(other.name)

    init {
        assert(name.isNotBlank())
    }
}
