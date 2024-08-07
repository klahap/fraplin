package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.*
import io.github.klahap.fraplin.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class FrappeSiteService(
    private val baseUrl: HttpUrl,
    private val client: OkHttpClient,
) {
    private val json: Json = Json { ignoreUnknownKeys = true }

    constructor(
        siteUrl: HttpUrl,
        userApiToken: String,
        client: OkHttpClient,
    ) : this(
        baseUrl = siteUrl,
        client = client.newBuilder().addInterceptor {
            it.proceed(it.request().newBuilder().header("Authorization", "token $userApiToken").build())
        }.build()
    )

    suspend fun getFraplinSpec(docTypeInfos: Set<DocTypeInfo>): FraplinSpec {
        val allDocTypeInfo = docTypeInfos + setOf(DocTypeInfo(name = "User"))
        val docTypeNames = allDocTypeInfo.map { it.name }.toSet()
        val allDocTypes = getDocTypes(allDocTypeInfo).toList().associateBy { it.docTypeName }
        if (!allDocTypes.keys.containsAll(docTypeNames))
            throw Exception("doc types not found: ${docTypeNames - allDocTypes.keys}")

        val baseDocTypes = allDocTypes.filterKeys { docTypeNames.contains(it) }.values.toList()
        val childDocTypes = baseDocTypes.asSequence().flatMap { it.fields }
            .filterIsInstance<DocField.Table>()
            .map { it.option }.toSet()
            .let { it - baseDocTypes.map { d -> d.docTypeName }.toSet() }
            .map { allDocTypes[it]!! }

        val docTypesGen = (baseDocTypes + childDocTypes).sortedBy { it.docTypeName }
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
            .sortedBy { it.docTypeName }
        return FraplinSpec(
            docTypes = docTypesGen,
            dummyDocTypes = dummyDocTypes
        )
    }

    private suspend fun getDocTypes(additionalInfo: Set<DocTypeInfo>): Flow<DocType.Full> =
        coroutineScope {
            withContext(Dispatchers.IO) {
                val docTypes = async {
                    loadBatches(docType = "DocType", batchSize = 1000) {
                        addQueryParameter("fields", getFilterList<DocTypeRaw>())
                    }.map { json.decodeFromJsonElement<DocTypeRaw>(it) }
                }
                val docFields = async {
                    loadBatches(docType = "DocField", batchSize = 1000) {
                        addQueryParameter("parent", "DocType")
                        addQueryParameter("fields", getFilterList<DocFieldRaw>())
                    }.map { json.decodeFromJsonElement<DocFieldRaw>(it) }
                }
                val docCustomFields = async {
                    loadBatches(docType = "Custom Field", batchSize = 1000) {
                        addQueryParameter("fields", getFilterList<DocCustomFieldRaw>())
                    }.map { json.decodeFromJsonElement<DocCustomFieldRaw>(it) }
                }
                val additionalInfoMap = additionalInfo.associateBy { it.name }
                val allFields = (docFields.await() + docCustomFields.await()).groupBy { it.parent }
                docTypes.await().map { docType ->
                    val fields = allFields[docType.name] ?: emptyList()
                    val info = additionalInfoMap[docType.name]
                    docType.toDocType(fields = fields, additionalInfo = info)
                }.asFlow()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun loadBatches(docType: String, batchSize: Int, block: HttpUrl.Builder.() -> Unit) =
        flow {
            for (idx in range()) {
                val names = requestBuilder {
                    get()
                    url(baseUrl.newBuilder {
                        addPathSegments("api/resource/$docType")
                        addQueryParameter("limit_start", (idx * batchSize).toString())
                        addQueryParameter("limit", batchSize.toString())
                        block()
                    })
                }.send(client) {
                    getJsonIfSuccessfulOrThrow<JsonObject>(json)["data"]!!.jsonArray.map { it.jsonObject }
                }
                emit(names)
                if (names.size != batchSize) break
            }
        }.flatMapConcat { it.asFlow() }.toSet()

    companion object {
        private fun Iterable<String>.toFilterList() =
            toSet().joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }

        private inline fun <reified T : Any> getFilterList() = getAllSerialNames<T>().toFilterList()
    }
}
