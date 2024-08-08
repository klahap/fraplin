package io.github.klahap.fraplin.models.config

import io.github.klahap.fraplin.models.DocTypeInfo
import kotlinx.serialization.Serializable

@Serializable
data class FraplinInputConfig(
    val source: FraplinSourceConfig,
    val docTypes: Set<DocTypeInfo>,
) {
    data class Builder(
        private var source: FraplinSourceConfig? = null,
        private val docTypes: MutableSet<DocTypeInfo> = mutableSetOf(),
    ) {
        fun addDocType(name: String, strictTyped: Boolean = false) {
            docTypes.add(DocTypeInfo(name = name, strictTyped = strictTyped))
        }

        fun sourceCloud(block: FraplinSourceConfig.Cloud.Builder.() -> Unit) {
            source = FraplinSourceConfig.Cloud.Builder().apply(block).build()
        }

        fun sourceSite(block: FraplinSourceConfig.Site.Builder.() -> Unit) {
            source = FraplinSourceConfig.Site.Builder().apply(block).build()
        }

        fun sourceRepo(block: FraplinSourceConfig.Repos.Builder.() -> Unit) {
            source = FraplinSourceConfig.Repos.Builder().apply(block).build()
        }

        fun build() = FraplinInputConfig(
            source = source ?: throw Exception("no source defined"),
            docTypes = docTypes.toSet().takeIf { it.isNotEmpty() } ?: throw Exception("no DocTypes defined"),
        )
    }
}
