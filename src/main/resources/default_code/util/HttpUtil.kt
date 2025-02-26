package default_code.util

import default_code.FraplinException
import default_code.FraplinResult
import io.github.goquati.kotlin.util.Success
import io.github.goquati.kotlin.util.getOr
import io.github.goquati.kotlin.util.map
import io.github.goquati.kotlin.util.mapError
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


fun HttpUrl.newBuilder(block: HttpUrl.Builder.() -> Unit) = newBuilder().apply(block).build()

inline fun <reified T> Response.getJson(json: Json = Json): FraplinResult<T> {
    getFraplinErrorOrNull()?.let { return it.result }
    val data = try {
        body.string()
    } catch (_: Exception) {
        return FraplinException.unprocessable("cannot read frappe response body").result
    }
    return json.decodeFromStringSafe(data)
}

inline fun <reified T : JsonElement> Response.getJson(json: Json = Json, key: String): FraplinResult<T> {
    val root = getJson<JsonObject>(json).getOr { return it.result }
    val value = root[key] ?: return FraplinException.unprocessable("JSON body has no root field '$key'").result
    if (value !is T) return FraplinException.unprocessable("JSON body has no root field '$key' of type '${T::class.simpleName}'").result
    return Success(value)
}

fun Response.takeFraplinResponseIfSuccess(): FraplinResult<Response> {
    if (isSuccessful) return Success(this)
    val prettyMessage = runCatching { message.takeIf(String::isNotBlank) ?: body.string() }.getOrDefault("")
    return FraplinException(status = code, msg = prettyMessage).result
}

fun Response.getFraplinErrorOrNull(): FraplinException? =
    takeFraplinResponseIfSuccess().failureOrNull

fun Response.getEmptyFraplinResult(msg: String? = null): FraplinResult<Unit> = takeFraplinResponseIfSuccess().map { }
    .mapError { it.copy(msg = listOfNotNull(msg, it.msg).joinToString(": ")) }

fun JsonElement.toRequestBody() =
    Json.encodeToString(this).toRequestBody("application/json; charset=utf-8".toMediaType())

fun Request.newBuilder(block: Request.Builder.(request: Request) -> Unit) =
    newBuilder().apply { block(this@newBuilder) }.build()

fun Headers.newBuilder(block: Headers.Builder.() -> Unit) = newBuilder().apply(block).build()
fun requestBuilder(block: Request.Builder.() -> Unit) = Request.Builder().apply(block).build()

fun multipartBody(block: MultipartBody.Builder.() -> Unit) =
    MultipartBody.Builder().apply(block).build()

fun Request.Builder.postMultipartBody(block: MultipartBody.Builder.() -> Unit) = post(multipartBody(block))
