package io.github.klahap.fraplin.services

import io.github.goquati.kotlin.util.intersectAll
import io.github.klahap.fraplin.models.*
import io.github.klahap.fraplin.models.config.FraplinInputConfig
import io.github.klahap.fraplin.models.config.FraplinSourceConfig
import okhttp3.OkHttpClient

class FraplinSpecService(
    private val client: OkHttpClient,
) {
    private val frappeGitRepoService get() = FrappeGitRepoService()
    suspend fun getSpec(config: FraplinInputConfig): FraplinSpec {
        val docTypeInfo = sequence {
            yieldAll(config.docTypes)
            config.openApiSpecs.forEach { spec -> yieldAll(spec.docTypes.map { it.docTypeInfo }) }
            yield(DocTypeInfo(name = "User"))
        }.toSet()
        val docTypeNames = docTypeInfo.map { it.docTypeName }.toSet()

        val schema = when (config.source) {
            is FraplinSourceConfig.Cloud -> config.source.getFrappeSite().getDocTypes(docTypeInfo)
            is FraplinSourceConfig.Site -> config.source.getFrappeSite().getDocTypes(docTypeInfo)
            is FraplinSourceConfig.Repos -> frappeGitRepoService.getAll(config.source.repos, docTypeInfo)
        }
        val allDocTypes = schema.docTypes.associateBy { it.docTypeName }

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

        val virtualDocTypes = docTypesGen.filterIsInstance<DocType.Virtual>().associateBy { it.docTypeName }
        val openApiSpecs = config.openApiSpecs.map { specConfig ->
            FraplinOpenApiSpec(
                name = specConfig.name,
                version = specConfig.version,
                pathPrefix = specConfig.pathPrefix,
                schemaPrefix = specConfig.schemaPrefix,
                pathTags = specConfig.pathTags,
                docTypes = specConfig.docTypes.sortedBy { it.docTypeName }.map { info ->
                    val docType = virtualDocTypes[info.docTypeName]
                        ?: throw Exception("virtual doc type '${info.docTypeName}' not found for OpenApi spec '${specConfig.name}'")
                    val validDataTypes = docType.dataTypes
                    val ignoreFieldsInAllDataTypes = validDataTypes
                        .map { info.ignoreFields[it] ?: emptyList() }
                        .intersectAll().toSet()

                    docType.copy(
                        fields = docType.fields.filter { it.fieldName !in ignoreFieldsInAllDataTypes }
                            .sortedBy { it.fieldName },
                        ignoreFields = info.ignoreFields
                            .filterKeys { it in validDataTypes }
                            .mapValues { it.value.sorted() }
                            .toSortedMap(),
                        ignoreEndpoints = info.ignoreEndpoints.sorted(),
                    )
                }
            )
        }

        return FraplinSpec(
            docTypes = docTypesGen.filterIsInstance<DocType.Base>().sortedBy { it.docTypeName },
            openApi = openApiSpecs,
            dummyDocTypes = dummyDocTypes.sortedBy { it.docTypeName },
            whiteListFunctions = schema.whiteListFunctions.sortedBy { it.name },
        )
    }

    private suspend fun FraplinSourceConfig.Cloud.getFrappeSite() =
        FrappeCloudBaseService(token = cloudToken, team = team, client = client).getSiteClient(url)

    private fun FraplinSourceConfig.Site.getFrappeSite() =
        FrappeSiteService(siteUrl = url, userApiToken = userToken, client = client)
}