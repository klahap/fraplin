package default_code.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class NotFoundException : Exception()

fun HttpUrl.newBuilder(block: HttpUrl.Builder.() -> Unit) = newBuilder().apply(block).build()

inline fun <reified T> Response.getJsonIfSuccessfulOrThrow(json: Json = Json): T {
    throwIfError()
    return json.decodeFromString(body!!.string())
}

fun Response.throwIfError() {
    if (isSuccessful) return
    if (code == 404) throw NotFoundException()
    throw Exception("$code, ${message.takeIf { it.isNotBlank() } ?: body?.string()}")
}

fun JsonElement.toRequestBody() =
    Json.encodeToString(this).toRequestBody("application/json; charset=utf-8".toMediaType())
