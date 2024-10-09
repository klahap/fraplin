package default_code.service

import default_code.*
import default_code.model.*
import default_code.model.filter.FrappeFilterString
import default_code.model.filter.FrappeFilterStringSet
import default_code.util.*
import io.github.goquati.kotlin.util.*
import io.github.goquati.kotlin.util.flatMap
import io.github.goquati.kotlin.util.toResultList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map
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
    private val additionalHeaderBuilder: suspend Headers.Builder.() -> FraplinResult<Unit>,
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
            Success(Unit)
        }
    )

    suspend fun <T, A, R> callWhiteListFun(
        fn: T,
        additionalArgs: Map<String, JsonElement> = emptyMap(),
        argBuilder: A.() -> JsonElement,
        body: RequestBody? = null,
        responseHandler: Response.() -> FraplinResult<R>,
    ): FraplinResult<R> where
            A : JsonElementField<*>,
            T : IWhiteListFun.Args.With<A>,
            T : IWhiteListFun.Scope = callWhiteListFunInternal(
        fn = fn,
        args = fn.getArgs(argBuilder) + additionalArgs,
        body = body,
        responseHandler = responseHandler
    )

    suspend fun <T, R> callWhiteListFun(
        fn: T,
        additionalArgs: Map<String, JsonElement> = emptyMap(),
        body: RequestBody? = null,
        responseHandler: Response.() -> FraplinResult<R>,
    ): FraplinResult<R> where
            T : IWhiteListFun.Args.Without,
            T : IWhiteListFun.Scope = callWhiteListFunInternal(
        fn = fn,
        args = additionalArgs,
        body = body,
        responseHandler = responseHandler
    )

    private suspend fun <T, R> callWhiteListFunInternal(
        fn: T,
        args: Map<String, JsonElement>,
        body: RequestBody?,
        responseHandler: Response.() -> FraplinResult<R>,
    ): FraplinResult<R> where T : IWhiteListFun.Args, T : IWhiteListFun.Scope {
        return requestBuilder {
            if (body == null) {
                post(JsonObject(args).toRequestBody())
                url(getFunUrl(fn, args = emptyMap()))
            }
            else {
                post(body)
                url(getFunUrl(fn, args = args))
            }
        }.run {
            when (fn as IWhiteListFun.Scope) {
                is IWhiteListFun.Scope.Private -> send(withAuthorization = true, responseHandler = responseHandler)
                is IWhiteListFun.Scope.Public -> send(withAuthorization = false, responseHandler = responseHandler)
            }
        }
    }

    private data class ChildObjectKey(
        val parent: FrappeDocTypeObjectName,
        val parentField: FrappeFieldName,
    )

    suspend fun <T> load(docType: KClass<T>): FraplinResult<T> where T : DocType.Single =
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
    ): FraplinResult<T> where T : DocType, T : DocTypeAbility.Query = _load(docType = docType, name = name)

    suspend inline fun <reified T> load(
        name: String,
    ): FraplinResult<T> where T : DocType, T : DocTypeAbility.Query =
        load(docType = T::class, name = name)

    suspend inline fun <reified T> load(
        link: FrappeLinkField<T>,
    ): FraplinResult<T> where T : DocType, T : DocTypeAbility.Query =
        load(docType = link.docType, name = link.value)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> loadAll(
        docType: KClass<T>,
        block: FrappeRequestOptions.Builder<T>.() -> Unit = {},
    ): Flow<FraplinResult<T>> where T : DocType, T : DocTypeAbility.Query {
        val deserializer = docType.toSerializer()
        return loadBatches(
            docType = docType.getDocTypeName(),
            batchSize = 50,
            options = FrappeRequestOptions.Builder<T>().apply(block).build(),
        ).flatMapConcat { objects ->
            if (objects.isFailure) return@flatMapConcat flowOf(objects.asFailure)
            val objectPairs = objects.success.map { obj ->
                val key = obj.getStringField("name").getOr { return@flatMapConcat flowOf(it.err) }
                FrappeDocTypeObjectName(key) to obj
            }
            val childs = loadChilds(
                parentDocType = docType,
                parentNames = objectPairs.map { it.first }.toSet(),
            ).getOr { return@flatMapConcat flowOf(it.err) }
            objectPairs.asFlow().map { (parentName, obj) ->
                val data = obj.toMutableMap()
                childs[parentName]?.forEach { (fieldName, fieldData) ->
                    data[fieldName.name] = fieldData
                }
                Success(JsonObject(data))
            }
        }.map { obj ->
            obj.flatMap {
                json.decodeFromJsonElementSafe(deserializer = deserializer, element = it)
            }
        }
    }

    suspend inline fun <reified T> loadAll(
        noinline block: FrappeRequestOptions.Builder<T>.() -> Unit = {},
    ): Flow<FraplinResult<T>> where T : DocType, T : DocTypeAbility.Query = loadAll(
        docType = T::class,
        block = block,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> loadAllNames(
        docType: KClass<T>,
        block: FrappeRequestOptions.Builder<T>.() -> Unit = {},
    ): Flow<FraplinResult<String>> where T : DocType, T : DocTypeAbility.Query = loadBatches(
        docType = docType.getDocTypeName(),
        batchSize = 1000,
        options = FrappeRequestOptions.Builder<T>().apply(block).build(),
        onlyNames = true,
    ).flatMapConcat { batch ->
        when {
            batch.isSuccess -> batch.success.map { it.getStringField("name") }.asFlow()
            else -> flowOf(batch.asFailure)
        }
    }

    suspend fun <T> create(
        docType: KClass<T>,
        data: JsonElement,
    ): FraplinResult<T> where T : DocType, T : DocTypeAbility.Create {
        return requestBuilder {
            post(data.toRequestBody())
            url(getDocTypeUrl(docType))
        }.send {
            getJson<JsonElement>(key = "data").flatMap {
                json.decodeFromJsonElementSafe(docType.toSerializer(), it)
            }
        }
    }

    suspend fun <T> update(
        docType: KClass<T>,
        name: String,
        data: JsonElement,
    ): FraplinResult<T> where T : DocType, T : DocTypeAbility.Update {
        return requestBuilder {
            put(data.toRequestBody())
            url(getDocTypeUrl(docType, name = name))
        }.send {
            getJson<JsonElement>(key = "data").flatMap {
                json.decodeFromJsonElementSafe(docType.toSerializer(), it)
            }
        }
    }

    suspend fun <T> delete(
        docType: KClass<T>,
        name: String,
    ): FraplinResult<Unit> where T : DocType, T : DocTypeAbility.Delete {
        return requestBuilder {
            delete()
            url(getDocTypeUrl(docType, name = name))
        }.send { getFraplinErrorOrNull()?.err ?: Success(Unit) }
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
    ): FraplinResult<T> where T : DocType {
        return requestBuilder {
            get()
            url(getDocTypeUrl(docType, name = name))
        }.send {
            getJson<JsonElement>(key = "data").flatMap {
                json.decodeFromJsonElementSafe(docType.toSerializer(), it)
            }
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
            ).flatMapConcat { chunk ->
                when {
                    chunk.isSuccess -> chunk.success.asFlow().map { child ->
                        val parent = child.getStringField("parent").getOr { return@map it.err }
                        val parentField = child.getStringField("parentfield").getOr { return@map it.err }
                        val childKey = ChildObjectKey(
                            parent = FrappeDocTypeObjectName(parent),
                            parentField = FrappeFieldName(parentField),
                        )
                        Success(childKey to child)
                    }

                    else -> flowOf(chunk.asFailure)
                }
            }.toResultList().map { batch ->
                batch.groupBy({ it.first }, { it.second })
                    .map { it.key to JsonArray(it.value) }
            }
        }.toResultList().map { batches ->
            batches.flatten()
                .groupBy({ it.first.parent }, { it.first.parentField to it.second })
                .mapValues { it.value.toMap() }
        }

    private suspend fun loadBatches(
        docType: FrappeDocTypeName,
        batchSize: Int,
        options: FrappeRequestOptions,
        onlyNames: Boolean = false
    ): Flow<FraplinResult<List<JsonObject>>> {
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
                    getJson<JsonArray>(key = "data").getOr { return@send it.err }.map {
                        it as? JsonObject
                            ?: return@send FraplinError.unprocessable("invalid child data array element").err
                    }.let { Success(it) }
                }
                emit(objects)
                if (objects.map { it.size }.getOr(-1) != batchSize) break
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

    private fun getFunUrl(
        fn: IWhiteListFun,
        args: Map<String, JsonElement>,
    ) = baseUrl.newBuilder {
        addPathSegment("api/method/${fn.name}")
        args.forEach { (key, value) ->
            addQueryParameter(key, value.toString())
        }
    }


    suspend fun <D : DocType.Single> uploadFile(
        body: RequestBody,
        fileName: String,
        isPrivate: Boolean,
        docType: KClass<D>,
        fieldName: KProperty1<D, FrappeAttachField?>,
    ): FraplinResult<FrappeUploadFileResponse> = uploadFile(
        body = body,
        fileName = fileName,
        isPrivate = isPrivate,
        link = object : FrappeLinkField<D> {
            override val docType: KClass<D> = docType
            override val value: String = docType.getDocTypeName().name
        },
        fieldName = fieldName,
    )

    suspend fun <D : DocType> uploadFile(
        body: RequestBody,
        fileName: String,
        isPrivate: Boolean,
        link: FrappeLinkField<D>,
        fieldName: KProperty1<D, FrappeAttachField?>,
    ): FraplinResult<FrappeUploadFileResponse> = upload {
        setDefaultFileUploadArgs(
            body = body,
            fileName = fileName,
            isPrivate = isPrivate,
            link = link,
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
    ): FraplinResult<FrappeUploadFileResponse> = uploadImage(
        body = body,
        fileName = fileName,
        isPrivate = isPrivate,
        optimize = optimize,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        link = object : FrappeLinkField<D> {
            override val docType: KClass<D> = docType
            override val value: String = docType.getDocTypeName().name
        },
        fieldName = fieldName,
    )

    suspend fun <D : DocType> uploadImage(
        body: RequestBody,
        fileName: String,
        isPrivate: Boolean,
        optimize: Boolean,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        link: FrappeLinkField<D>,
        fieldName: KProperty1<D, FrappeAttachField?>,
    ): FraplinResult<FrappeUploadFileResponse> = upload {
        setDefaultFileUploadArgs(
            body = body,
            fileName = fileName,
            isPrivate = isPrivate,
            link = link,
            fieldName = fieldName,
        )
        addFormDataPart("optimize", if (optimize) "1" else "0")
        if (maxWidth != null)
            addFormDataPart("max_width", maxWidth.toString())
        if (maxHeight != null)
            addFormDataPart("max_height", maxHeight.toString())
    }

    private suspend fun upload(block: MultipartBody.Builder.() -> Unit): FraplinResult<FrappeUploadFileResponse> =
        requestBuilder {
            postMultipartBody { block() }
            url("$baseUrl/api/method/upload_file")
        }.send {
            getJson<JsonElement>(key = "message").flatMap {
                json.decodeFromJsonElementSafe<FrappeUploadFileResponse>(it)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : DocType> KClass<T>.toSerializer() =
        json.serializersModule.serializer(createType()) as KSerializer<T>

    @OptIn(ExperimentalCoroutinesApi::class)
    protected suspend fun <T> Request.send(
        withAuthorization: Boolean = true,
        responseHandler: Response.() -> FraplinResult<T>,
    ): FraplinResult<T> {
        val headerBuilder = Headers.Builder().addAll(this@send.headers)
        if (withAuthorization)
            additionalHeaderBuilder(headerBuilder).getOr { return it.err }
        val headersToAdd = headerBuilder.build()
        val request = newBuilder { headers(headersToAdd) }
        return baseClient.newCall(request).executeAsync().use { responseHandler(it) }
    }

    protected suspend fun Request.send(
        withAuthorization: Boolean = true,
    ) = send(withAuthorization = withAuthorization) { Success(Unit) }

    companion object {
        private fun JsonObject.getStringField(key: String): FraplinResult<String> {
            val field = this[key]
                ?: return FraplinError.unprocessable("frappe response json has no field '$key'").err
            if (field !is JsonPrimitive || !field.isString)
                return FraplinError.unprocessable("frappe response json has no field '$key' of type String").err
            return Success(field.content)
        }

        private fun <D : DocType> MultipartBody.Builder.setDefaultFileUploadArgs(
            body: RequestBody,
            fileName: String,
            isPrivate: Boolean,
            link: FrappeLinkField<D>,
            fieldName: KProperty1<D, FrappeAttachField?>,
        ) {
            setType(MultipartBody.FORM)
            addFormDataPart("file", fileName, body)
            addFormDataPart("is_private", if (isPrivate) "1" else "0")
            addFormDataPart("doctype", link.docType.getDocTypeName().name)
            addFormDataPart("docname", link.value)
            addFormDataPart("fieldname", fieldName.name)
        }
    }
}