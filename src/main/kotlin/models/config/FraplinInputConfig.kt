package io.github.klahap.fraplin.models.config

import io.github.klahap.fraplin.models.DocTypeInfo
import kotlinx.serialization.Serializable

@Serializable
data class FraplinInputConfig(
    val source: FraplinSourceConfig,
    val docTypes: Set<DocTypeInfo>,
    val openApiSpecs: List<FraplinOpenApiSpecConfig>,
) {
    data class Builder(
        private var source: FraplinSourceConfig? = null,
        private val docTypes: MutableSet<DocTypeInfo> = mutableSetOf(),
        private val openApiSpecs: MutableList<FraplinOpenApiSpecConfig> = mutableListOf(),
    ) {
        fun addDocType(name: String, strictTyped: Boolean = false) {
            DocTypeInfo(name = name, strictTyped = strictTyped)
                .also { docTypes.add(it) }
        }

        fun openApiSpec(name: String, block: FraplinOpenApiSpecConfig.Builder.() -> Unit) {
            openApiSpecs.add(FraplinOpenApiSpecConfig.Builder(name = name).apply(block).build())
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
            docTypes = docTypes.toSortedSet(),
            openApiSpecs = openApiSpecs.sortedBy { it.name }
        )
    }
}
