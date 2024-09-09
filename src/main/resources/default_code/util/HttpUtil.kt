package default_code.util

import default_code.FraplinError
import default_code.FraplinResult
import io.github.goquati.kotlin.util.Success
import io.github.goquati.kotlin.util.getOr
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


fun HttpUrl.newBuilder(block: HttpUrl.Builder.() -> Unit) = newBuilder().apply(block).build()

inline fun <reified T> Response.getJson(json: Json = Json): FraplinResult<T> {
    getFraplinErrorOrNull()?.let { return it.err }
    val data = try {
        body!!.string()
    } catch (e: Exception) {
        return FraplinError(status = 422, "cannot read frappe response body").err
    }
    return json.decodeFromStringSafe(data)
}

inline fun <reified T : JsonElement> Response.getJson(json: Json = Json, key: String): FraplinResult<T> {
    getFraplinErrorOrNull()?.let { return it.err }
    val data = try {
        body!!.string()
    } catch (e: Exception) {
        return FraplinError(status = 422, "cannot read frappe response body").err
    }
    val root = json.decodeFromStringSafe<JsonObject>(data).getOr { return it.err }
    val value = root[key] ?: return FraplinError.unprocessable("JSON body has no root field '$key'").err
    if (value !is T) return FraplinError.unprocessable("JSON body has no root field '$key' of type '${T::class.simpleName}'").err
    return Success(value)
}

fun Response.getFraplinErrorOrNull(): FraplinError? {
    if (isSuccessful) return null
    return FraplinError(status = code, msg = prettyMessage)
}

fun Response.getEmptyFraplinResult(msg: String? = null): FraplinResult<Unit> {
    if (isSuccessful) return Success(Unit)
    return FraplinError(status = code, msg = (msg?.let { "$it, " } ?: "") + prettyMessage).err
}

val Response.prettyMessage get() = message.takeIf { it.isNotBlank() } ?: body?.string() ?: ""

fun JsonElement.toRequestBody() =
    Json.encodeToString(this).toRequestBody("application/json; charset=utf-8".toMediaType())

fun Request.newBuilder(block: Request.Builder.(request: Request) -> Unit) =
    newBuilder().apply { block(this@newBuilder) }.build()

fun Headers.newBuilder(block: Headers.Builder.() -> Unit) = newBuilder().apply(block).build()
fun requestBuilder(block: Request.Builder.() -> Unit) = Request.Builder().apply(block).build()

suspend fun headerBuilder(block: suspend Headers.Builder.() -> Unit) = Headers.Builder().apply { block() }.build()

fun multipartBody(block: MultipartBody.Builder.() -> Unit) =
    MultipartBody.Builder().apply(block).build()

fun Request.Builder.postMultipartBody(block: MultipartBody.Builder.() -> Unit) = post(multipartBody(block))
