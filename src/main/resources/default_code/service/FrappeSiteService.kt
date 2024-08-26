package default_code.service

import default_code.DocType
import default_code.DocTypeAbility
import default_code.IWhiteListFun
import default_code.model.*
import default_code.model.filter.FrappeFilterString
import default_code.model.filter.FrappeFilterStringSet
import default_code.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import okhttp3.*
import okhttp3.coroutines.executeAsync
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType


open class FrappeSiteService(
    protected val baseUrl: HttpUrl,
    private val additionalHeaderBuilder: suspend Headers.Builder.() -> Unit,
    private val baseClient: OkHttpClient,
) {
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }

    constructor(
        siteUrl: HttpUrl,
        userApiToken: String,
        httpClient: OkHttpClient = OkHttpClient(),
    ) : this(
        baseUrl = siteUrl,
        baseClient = httpClient,
        additionalHeaderBuilder = {
            add("Authorization", "token $userApiToken")
        }
    )

    suspend fun <T, A, R> callWhiteListFun(
        fn: T,
        additionalArgs: Map<String, JsonElement>,
        argBuilder: A.() -> Pair<String, JsonElement>,
        responseHandler: Response.() -> R,
    ): R where
            A : JsonElementField<*>,
            T : IWhiteListFun.Args.With<A>,
            T : IWhiteListFun.Scope = _callWhiteListFun(
        fn = fn,
        args = fn.getArgs(argBuilder) + additionalArgs,
        responseHandler = responseHandler
    )

    suspend fun <T, R> callWhiteListFun(
        fn: T,
        additionalArgs: Map<String, JsonElement>,
        responseHandler: Response.() -> R,
    ): R where
            T : IWhiteListFun.Args.Without,
            T : IWhiteListFun.Scope = _callWhiteListFun(
        fn = fn,
        args = additionalArgs,
        responseHandler = responseHandler
    )

    private suspend fun <T, R> _callWhiteListFun(
        fn: T,
        args: Map<String, JsonElement>,
        responseHandler: Response.() -> R,
    ): R where T : IWhiteListFun.Args, T : IWhiteListFun.Scope {
        return requestBuilder {
            post(JsonObject(args).toRequestBody())
            url(getFunUrl(fn))
        }.run {
            when (fn as IWhiteListFun.Scope) {
                is IWhiteListFun.Scope.Private -> send(responseHandler = responseHandler)
                is IWhiteListFun.Scope.Public -> send(responseHandler = responseHandler)
            }
        }
    }

    private data class ChildObjectKey(
        val parent: FrappeDocTypeObjectName,
        val parentField: FrappeFieldName,
    )

    suspend fun <T> load(docType: KClass<T>): T where T : DocType.Single =
        _load(docType = docType, name = docType.getDocTypeName().name)

    suspend inline fun <reified T> load() where T : DocType.Single =
        load(docType = T::class)

    suspend fun <T> exists(
        docType: KClass<T>,
        name: String,
    ): Boolean where T : DocType, T : DocTypeAbility.Query = loadAllNames(docType = docType) {
        filters {
            add(
                FrappeFilter(
                    fieldName = "name",
                    operator = FrappeFilter.Operator.Eq,
                    value = FrappeFilterString(name),
                )
            )
        }
    }.count() == 1

    suspend inline fun <reified T> exists(
        name: String,
    ): Boolean where T : DocType, T : DocTypeAbility.Query =
        exists(docType = T::class, name = name)

    suspend inline fun <reified T> exists(
        link: FrappeLinkField<T>,
    ): Boolean where T : DocType, T : DocTypeAbility.Query =
        exists(docType = link.docType, name = link.value)

    suspend fun <T> load(
        docType: KClass<T>,
        name: String,
    ): T where T : DocType, T : DocTypeAbility.Query = _load(docType = docType, name = name)

    suspend inline fun <reified T> load(
        name: String,
    ): T where T : DocType, T : DocTypeAbility.Query =
        load(docType = T::class, name = name)

    suspend inline fun <reified T> load(
        link: FrappeLinkField<T>,
    ): T where T : DocType, T : DocTypeAbility.Query =
        load(docType = link.docType, name = link.value)

    suspend fun <T> loadOrNull(
        docType: KClass<T>,
        name: String,
    ): T? where T : DocType, T : DocTypeAbility.Query =
        runCatching { _load(docType = docType, name = name) }
            .onFailure { if (it !is HttpException.ClientError.NotFound) throw it }
            .getOrNull()

    suspend inline fun <reified T> loadOrNull(
        name: String,
    ) where T : DocType, T : DocTypeAbility.Query =
        loadOrNull(docType = T::class, name = name)

    suspend inline fun <reified T> loadOrNull(
        link: FrappeLinkField<T>,
    ) where T : DocType, T : DocTypeAbility.Query =
        loadOrNull(docType = link.docType, name = link.value)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> loadAll(
        docType: KClass<T>,
        block: FrappeRequestOptions.Builder<T>.() -> Unit = {},
    ): Flow<T> where T : DocType, T : DocTypeAbility.Query {
        val deserializer = docType.toSerializer()
        return loadBatches(
            docType = docType.getDocTypeName(),
            batchSize = 50,
            options = FrappeRequestOptions.Builder<T>().apply(block).build(),
        )
            .map { objects -> objects.map { FrappeDocTypeObjectName(it["name"]!!.jsonPrimitive.content) to it } }
            .flatMapConcat { objects ->
                val childs = loadChilds(
                    parentDocType = docType,
                    parentNames = objects.map { it.first }.toSet(),
                )
                objects.asFlow().map { (parentName, obj) ->
                    val data = obj.toMutableMap()
                    childs[parentName]?.forEach { (fieldName, fieldData) ->
                        data[fieldName.name] = fieldData
                    }
                    JsonObject(data)
                }
            }
            .map { json.decodeFromJsonElement(deserializer = deserializer, element = it) }
    }

    suspend inline fun <reified T> loadAll(
        noinline block: FrappeRequestOptions.Builder<T>.() -> Unit = {},
    ): Flow<T> where T : DocType, T : DocTypeAbility.Query = loadAll(
        docType = T::class,
        block = block,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> loadAllNames(
        docType: KClass<T>,
        block: FrappeRequestOptions.Builder<T>.() -> Unit = {},
    ): Flow<String> where T : DocType, T : DocTypeAbility.Query = loadBatches(
        docType = docType.getDocTypeName(),
        batchSize = 1000,
        options = FrappeRequestOptions.Builder<T>().apply(block).build(),
        onlyNames = true,
    ).flatMapConcat { batch ->
        batch.map { it["name"]!!.jsonPrimitive.content }.asFlow()
    }

    suspend fun <T> create(
        docType: KClass<T>,
        data: JsonElement,
    ): T where T : DocType, T : DocTypeAbility.Create {
        return requestBuilder {
            post(data.toRequestBody())
            url(getDocTypeUrl(docType))
        }.send {
            getJsonIfSuccessfulOrThrow<JsonObject>(json)["data"]!!
                .let { json.decodeFromJsonElement(docType.toSerializer(), it) }
        }
    }

    suspend fun <T> update(
        docType: KClass<T>,
        name: String,
        data: JsonElement,
    ): T where T : DocType, T : DocTypeAbility.Update {
        return requestBuilder {
            put(data.toRequestBody())
            url(getDocTypeUrl(docType, name = name))
        }.send {
            getJsonIfSuccessfulOrThrow<JsonObject>(json)["data"]!!
                .let { json.decodeFromJsonElement(docType.toSerializer(), it) }
        }
    }

    suspend fun <T> delete(
        docType: KClass<T>,
        name: String,
    ) where T : DocType, T : DocTypeAbility.Delete {
        return requestBuilder {
            delete()
            url(getDocTypeUrl(docType, name = name))
        }.send { throwIfError() }
    }

    suspend inline fun <reified T> delete(
        name: String,
    ) where T : DocType, T : DocTypeAbility.Delete = delete(T::class, name)

    suspend inline fun <reified T> delete(
        link: FrappeLinkField<T>,
    ) where T : DocType, T : DocTypeAbility.Delete =
        delete(docType = link.docType, name = link.value)

    private suspend fun <T> _load(
        docType: KClass<T>,
        name: String?,
    ): T where T : DocType {
        return requestBuilder {
            get()
            url(getDocTypeUrl(docType, name = name))
        }.send {
            json.decodeFromJsonElement(
                deserializer = docType.toSerializer(),
                element = getJsonIfSuccessfulOrThrow<JsonObject>(json)["data"]!!,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T> loadChilds(
        parentDocType: KClass<T>,
        parentNames: Set<FrappeDocTypeObjectName>,
    ) where T : DocType, T : DocTypeAbility.Query =
        parentDocType.getTableFields().runParallel(Dispatchers.IO) { field ->
            loadBatches(
                docType = field.childDocType,
                batchSize = 100,
                options = FrappeRequestOptions(
                    parent = parentDocType.getDocTypeName(),
                    filters = FrappeFilterSet(
                        FrappeFilter(
                            fieldName = "parent",
                            operator = FrappeFilter.Operator.In,
                            value = FrappeFilterStringSet(parentNames.map { it.name }.toSet()),
                        )
                    )
                )
            ).flatMapConcat { it.asFlow() }.toList().groupBy {
                ChildObjectKey(
                    parent = FrappeDocTypeObjectName(
                        it["parent"]?.jsonPrimitive?.content
                            ?: throw Exception("update frappe, bugfix: https://github.com/frappe/frappe/pull/26543")
                    ),
                    parentField = FrappeFieldName(
                        it["parentfield"]?.jsonPrimitive?.content
                            ?: throw Exception("update frappe, bugfix: https://github.com/frappe/frappe/pull/26543")
                    ),
                )
            }.map { it.key to JsonArray(it.value) }
        }.flatten()
            .groupBy({ it.first.parent }, { it.first.parentField to it.second })
            .mapValues { it.value.toMap() }

    private suspend fun loadBatches(
        docType: FrappeDocTypeName,
        batchSize: Int,
        options: FrappeRequestOptions,
        onlyNames: Boolean = false
    ): Flow<List<JsonObject>> {
        val url = getDocTypeUrl(docType).newBuilder {
            if (!onlyNames)
                addQueryParameter("fields", """["*"]""")
            options.filters?.let { addQueryParameter("filters", it.serialize()) }
            options.parent?.let { addQueryParameter("parent", it.name) }
            options.orderBy?.let { addQueryParameter("order_by", it.serialize()) }
        }
        return flow {
            for (idx in generateSequence(0) { it + 1 }) {
                val objects = requestBuilder {
                    get()
                    url(url.newBuilder {
                        addQueryParameter("limit_start", (idx * batchSize).toString())
                        addQueryParameter("limit", batchSize.toString())
                    })
                }.send {
                    getJsonIfSuccessfulOrThrow<JsonObject>(json)["data"]!!.jsonArray.map { it.jsonObject }
                }
                emit(objects)
                if (objects.size != batchSize) break
            }
        }
    }

    private fun <T : DocType> getDocTypeUrl(docType: KClass<T>, name: String? = null) =
        getDocTypeUrl(docType.getDocTypeName(), name = name)

    private fun getDocTypeUrl(docType: FrappeDocTypeName, name: String? = null) =
        baseUrl.newBuilder {
            addPathSegment("api/resource/$docType")
            if (name != null)
                addPathSegment(name)
        }

    private fun getFunUrl(fn: IWhiteListFun) = baseUrl.newBuilder {
        addPathSegment("api/method/${fn.name}")
    }


    suspend fun <D : DocType.Single> uploadFile(
        body: RequestBody,
        fileName: String,
        isPrivate: Boolean,
        docType: KClass<D>,
        fieldName: KProperty1<D, FrappeAttachField?>,
    ): FrappeUploadFileResponse = uploadFile(
        body = body,
        fileName = fileName,
        isPrivate = isPrivate,
        docType = docType,
        docName = docType.getDocTypeName().name,
        fieldName = fieldName,
    )

    suspend fun <D : DocType> uploadFile(
        body: RequestBody,
        fileName: String,
        isPrivate: Boolean,
        docType: KClass<D>,
        docName: String,
        fieldName: KProperty1<D, FrappeAttachField?>,
    ): FrappeUploadFileResponse = upload {
        setDefaultFileUploadArgs(
            body = body,
            fileName = fileName,
            isPrivate = isPrivate,
            docType = docType,
            docName = docName,
            fieldName = fieldName,
        )
    }

    suspend fun <D : DocType.Single> uploadImage(
        body: RequestBody,
        fileName: String,
        isPrivate: Boolean,
        optimize: Boolean,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        docType: KClass<D>,
        fieldName: KProperty1<D, FrappeAttachField?>,
    ): FrappeUploadFileResponse = uploadImage(
        body = body,
        fileName = fileName,
        isPrivate = isPrivate,
        optimize = optimize,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        docType = docType,
        docName = docType.getDocTypeName().name,
        fieldName = fieldName,
    )

    suspend fun <D : DocType> uploadImage(
        body: RequestBody,
        fileName: String,
        isPrivate: Boolean,
        optimize: Boolean,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        docType: KClass<D>,
        docName: String,
        fieldName: KProperty1<D, FrappeAttachField?>,
    ): FrappeUploadFileResponse = upload {
        setDefaultFileUploadArgs(
            body = body,
            fileName = fileName,
            isPrivate = isPrivate,
            docType = docType,
            docName = docName,
            fieldName = fieldName,
        )
        addFormDataPart("optimize", if (optimize) "1" else "0")
        if (maxWidth != null)
            addFormDataPart("max_width", maxWidth.toString())
        if (maxHeight != null)
            addFormDataPart("max_height", maxHeight.toString())
    }

    private suspend fun upload(block: MultipartBody.Builder.() -> Unit): FrappeUploadFileResponse = requestBuilder {
        postMultipartBody { block() }
        url("$baseUrl/api/method/upload_file")
    }.send {
        getJsonIfSuccessfulOrThrow<JsonObject>(json = json)["message"]!!.let {
            json.decodeFromJsonElement<FrappeUploadFileResponse>(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : DocType> KClass<T>.toSerializer() =
        json.serializersModule.serializer(createType()) as KSerializer<T>

    @OptIn(ExperimentalCoroutinesApi::class)
    protected suspend fun <T> Request.send(
        withAuthorization: Boolean = true,
        responseHandler: Response.() -> T,
    ): T {
        val headersToAdd = headerBuilder {
            if (withAuthorization) additionalHeaderBuilder()
            addAll(this@send.headers)
        }
        val request = newBuilder { headers(headersToAdd) }
        return baseClient.newCall(request).executeAsync().use { responseHandler(it) }
    }

    protected suspend fun Request.send(
        withAuthorization: Boolean = true,
    ) = send(withAuthorization = withAuthorization) {}

    companion object {
        private fun <D : DocType> MultipartBody.Builder.setDefaultFileUploadArgs(
            body: RequestBody,
            fileName: String,
            isPrivate: Boolean,
            docType: KClass<D>,
            docName: String,
            fieldName: KProperty1<D, FrappeAttachField?>,
        ) {
            setType(MultipartBody.FORM)
            addFormDataPart("file", fileName, body)
            addFormDataPart("is_private", if (isPrivate) "1" else "0")
            addFormDataPart("doctype", docType.getDocTypeName().name)
            addFormDataPart("docname", docName)
            addFormDataPart("fieldname", fieldName.name)
        }
    }
}