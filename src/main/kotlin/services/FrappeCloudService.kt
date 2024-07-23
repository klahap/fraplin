package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.util.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl

class FrappeCloudBaseService(
    private val token: String,
    private val team: String? = null,
    private val baseHttpClient: OkHttpClient = OkHttpClient(),
) {
    private val frappeCloudUrl = "https://frappecloud.com/api/method/press.api.site.login".toHttpUrl()

    suspend fun getSiteClient(siteUrl: HttpUrl): FrappeSiteService {
        val body = JsonObject(mapOf("name" to JsonPrimitive(siteUrl.host))).toRequestBody()
        val sid = requestBuilder {
            post(body)
            url(frappeCloudUrl)
            header("Authorization", "Token $token")
            team?.takeIfNotBlank()?.let {
                header("X-Press-Team", it)
            }
        }.send(baseHttpClient) {
            getJsonIfSuccessfulOrThrow<JsonObject>()["message"]!!.jsonObject["sid"]!!.jsonPrimitive.content
        }
        return FrappeSiteService(
            baseUrl = siteUrl,
            client = baseHttpClient.newBuilder().addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder {
                    header("Cookie", "sid=$sid")
                    team?.takeIfNotBlank()?.let {
                        header("X-Press-Team", it)
                    }
                })
            }.build(),
        )
    }
}