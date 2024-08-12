package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.DocField
import io.github.klahap.fraplin.models.DocType
import io.github.klahap.fraplin.models.DocTypeInfo
import io.github.klahap.fraplin.models.FraplinSpec
import io.github.klahap.fraplin.models.config.FraplinInputConfig
import io.github.klahap.fraplin.models.config.FraplinSourceConfig
import okhttp3.OkHttpClient

class FraplinSpecService(
    private val client: OkHttpClient,
) {
    private val frappeGitRepoService get() = FrappeGitRepoService()
    suspend fun getSpec(config: FraplinInputConfig): FraplinSpec {
        val docTypeInfo = config.docTypes + setOf(DocTypeInfo(name = "User"))
        val docTypeNames = docTypeInfo.map { it.docTypeName }.toSet()

        val allDocTypes = when (config.source) {
            is FraplinSourceConfig.Cloud -> config.source.getFrappeSite().getDocTypes(docTypeInfo)
            is FraplinSourceConfig.Site -> config.source.getFrappeSite().getDocTypes(docTypeInfo)
            is FraplinSourceConfig.Repos -> frappeGitRepoService.getAll(config.source.repos, docTypeInfo)
        }.toList().associateBy { it.docTypeName }

        if (!allDocTypes.keys.containsAll(docTypeNames))
            throw Exception("doc types not found: ${docTypeNames - allDocTypes.keys}")

        val baseDocTypes = allDocTypes.filterKeys { docTypeNames.contains(it) }.values.toList()
        val childDocTypes = baseDocTypes.asSequence().flatMap { it.fields }
            .filterIsInstance<DocField.Table>()
            .map { it.option }.toSet()
            .let { it - baseDocTypes.map { d -> d.docTypeName }.toSet() }
            .map { allDocTypes[it]!! }

        val docTypesGen = (baseDocTypes + childDocTypes)
        val docTypesGenNames = docTypesGen.map { it.docTypeName }.toSet()
        val docTypeDummyNames = docTypesGen.asSequence()
            .flatMap { it.fields }
            .filterIsInstance<DocField.Link>()
            .map { it.option }
            .filter { !docTypesGenNames.contains(it) }
            .toSet()
        val dummyDocTypes = allDocTypes
            .filterKeys { docTypeDummyNames.contains(it) }.values
            .map { it.toDummy() }
        return FraplinSpec(
            docTypes = docTypesGen.filterIsInstance<DocType.Base>().sortedBy { it.docTypeName },
            virtualDocTypes = docTypesGen.filterIsInstance<DocType.Virtual>().sortedBy { it.docTypeName },
            dummyDocTypes = dummyDocTypes.sortedBy { it.docTypeName }
        )
    }

    private suspend fun FraplinSourceConfig.Cloud.getFrappeSite() =
        FrappeCloudBaseService(token = cloudToken, team = team, client = client).getSiteClient(url)

    private fun FraplinSourceConfig.Site.getFrappeSite() =
        FrappeSiteService(siteUrl = url, userApiToken = userToken, client = client)
}