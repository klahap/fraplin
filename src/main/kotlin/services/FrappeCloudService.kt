package com.fraplin.services

import com.fraplin.util.getJsonIfSuccessfulOrThrow
import com.fraplin.util.send
import com.fraplin.util.toRequestBody
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl

class FrappeCloudBaseService(
    private val token: String,
    private val baseHttpClient: OkHttpClient = OkHttpClient(),
) {
    private val frappeCloudUrl = "https://frappecloud.com/api/method/press.api.site.login".toHttpUrl()

    suspend fun getSiteClient(siteUrl: HttpUrl): FrappeSiteService {
        val body = JsonObject(mapOf("name" to JsonPrimitive(siteUrl.host))).toRequestBody()
        val sid = Request.Builder()
            .post(body)
            .url(frappeCloudUrl)
            .header("Authorization", "Token $token")
            .send(baseHttpClient) {
                getJsonIfSuccessfulOrThrow<JsonObject>()["message"]!!.jsonObject["sid"]!!.jsonPrimitive.content
            }
        return FrappeSiteService(
            baseUrl = siteUrl,
            client = baseHttpClient.newBuilder().addInterceptor {
                it.proceed(it.request().newBuilder().header("Cookie", "sid=$sid").build())
            }.build(),
        )
    }
}