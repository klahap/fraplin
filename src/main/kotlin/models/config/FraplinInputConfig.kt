package io.github.klahap.fraplin.models.config

import io.github.klahap.fraplin.models.DocTypeInfo
import kotlinx.serialization.Serializable

@Serializable
data class FraplinInputConfig(
    val source: FraplinSourceConfig,
    val docTypes: Set<DocTypeInfo>,
) {
    data class Builder(
        var source: FraplinSourceConfig? = null,
        var docTypes: Set<DocTypeInfo> = mutableSetOf(),
    ) {
        fun build() = FraplinInputConfig(
            source = source ?: throw Exception("no source defined"),
            docTypes = docTypes.toSet().takeIf { it.isNotEmpty() } ?: throw Exception("no DocTypes defined"),
        )
    }
}
