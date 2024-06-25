package default_code.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

sealed class HttpException(msg: String) : Exception(msg) {
    sealed class ClientError(msg: String): HttpException(msg) {
        class NotFound : ClientError("404, not found")
        class Conflict : ClientError("409, conflict")
        class Other(msg: String): ClientError(msg)
    }
    sealed class ServerError(msg: String): HttpException(msg) {
        class InternalServerError: ServerError("500, Internal Server Error")
        class Other(msg: String): ServerError(msg)
    }
}

fun HttpUrl.newBuilder(block: HttpUrl.Builder.() -> Unit) = newBuilder().apply(block).build()

inline fun <reified T> Response.getJsonIfSuccessfulOrThrow(json: Json = Json): T {
    throwIfError()
    return json.decodeFromString(body!!.string())
}

fun Response.throwIfError() {
    if (isSuccessful) return
    val msg = message.takeIf { it.isNotBlank() } ?: body?.string()
    when(code) {
        404 -> throw HttpException.ClientError.NotFound()
        409 -> throw HttpException.ClientError.Conflict()
        500 -> throw HttpException.ServerError.InternalServerError()
        in 400..499 -> throw HttpException.ClientError.Other("$code, $msg")
        in 500..599 -> throw HttpException.ServerError.Other("$code, $msg")
    }
}

fun JsonElement.toRequestBody() =
    Json.encodeToString(this).toRequestBody("application/json; charset=utf-8".toMediaType())
