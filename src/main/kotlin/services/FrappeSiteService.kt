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

    suspend fun getDocTypes(additionalInfo: Set<DocTypeInfo>): FrappeSchema =
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
                        addQueryParameter("fields", getFilterList<DocFieldRaw.Common>())
                    }.map { json.decodeFromJsonElement<DocFieldRaw.Common>(it) }
                }
                val docCustomFields = async {
                    loadBatches(docType = "Custom Field", batchSize = 1000) {
                        addQueryParameter("fields", getFilterList<DocFieldRaw.Custom>())
                    }.map { json.decodeFromJsonElement<DocFieldRaw.Custom>(it) }
                }
                FrappeSchema.Collector(
                    docTypes = docTypes.await(),
                    docFields = docFields.await() + docCustomFields.await()
                ).collect(additionalInfo = additionalInfo)
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
