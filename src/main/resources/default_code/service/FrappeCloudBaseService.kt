package default_code.service

import default_code.FraplinError
import default_code.FraplinResult
import default_code.util.*
import io.github.goquati.kotlin.util.Success
import io.github.goquati.kotlin.util.flatMap
import io.github.goquati.kotlin.util.getOr
import io.github.goquati.kotlin.util.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.coroutines.executeAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

open class FrappeCloudBaseService(
    private val token: String,
    private val team: String? = null,
    private val baseClient: OkHttpClient = OkHttpClient().newBuilder().addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder { request ->
            headers(request.headers.newBuilder {
                add("Authorization", "Token $token")
                team?.takeIfNotBlank()?.let {
                    add("X-Press-Team", it)
                }
            })
        })
    }.build(),
) {

    private fun frappeCloudUrl(path: String) = "https://frappecloud.com/api/method/$path".toHttpUrl()
    private val siteTokens = ConcurrentHashMap<HttpUrl, CompletableFuture<FraplinResult<SiteToken>>>()

    val client: OkHttpClient
        get() = baseClient

    fun getSiteAuthHeaderBuilder(
        siteUrl: HttpUrl,
        siteToken: String? = null,
    ): suspend Headers.Builder.() -> FraplinResult<Unit> {
        return if (siteToken.isNullOrBlank()) {
            {
                getSiteToken(siteUrl).map { token ->
                    add("Cookie", "sid=${token.token}")
                }
            }
        } else {
            {
                add("Authorization", "token $siteToken")
                Success(Unit)
            }
        }
    }

    suspend fun existsSite(siteUrl: HttpUrl): Boolean = getSite(siteUrl).isSuccess

    suspend fun getSite(siteUrl: HttpUrl): FraplinResult<JsonObject> = Request.Builder()
        .post(JsonObject(mapOf("name" to JsonPrimitive(siteUrl.host))).toRequestBody())
        .url(frappeCloudUrl("press.api.site.get"))
        .send {
            getJson<JsonObject>(key = "message")
        }

    suspend fun getSiteUpdates(siteUrl: HttpUrl): FraplinResult<JsonObject> = Request.Builder()
        .post(JsonObject(mapOf("name" to JsonPrimitive(siteUrl.host))).toRequestBody())
        .url(frappeCloudUrl("press.api.site.check_for_updates"))
        .send {
            getJson<JsonObject>(key = "message")
        }

    suspend fun getSiteToken(siteUrl: HttpUrl): FraplinResult<SiteToken> {
        val siteToken = siteTokens.getValueForKey(siteUrl) { url ->
            val body = JsonObject(mapOf("name" to JsonPrimitive(url.host))).toRequestBody()
            val token = Request.Builder()
                .post(body)
                .url(frappeCloudUrl("press.api.site.login"))
                .send {
                    getJson<JsonObject>(key = "message").flatMap { it.getStringField("sid") }
                }.getOr { return@getValueForKey it.err }
            SiteToken(
                token = token,
                expiresIn = now.plusHours(3 * 24 - 1),
            ).let { Success(it) }
        }.getOr {
            siteTokens.remove(siteUrl)
            return it.err
        }
        return if (siteToken.isExpired()) {
            siteTokens.remove(siteUrl)
            getSiteToken(siteUrl)
        } else
            Success(siteToken)
    }

    data class SiteToken(
        val token: String,
        val expiresIn: LocalDateTime,
    ) {
        fun isExpired() = expiresIn < now
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    protected suspend fun <T> Request.Builder.send(responseHandler: Response.() -> T): T =
        baseClient.newCall(this.build()).executeAsync().use { responseHandler(it) }

    companion object {
        private val now get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        private fun LocalDateTime.plusHours(hours: Long) =
            toJavaLocalDateTime().plusHours(hours).toKotlinLocalDateTime()

        private fun JsonObject.getStringField(key: String): FraplinResult<String> {
            val field = this[key]
                ?: return FraplinError.unprocessable("frappe cloud response json has no field '$key'").err
            if (field !is JsonPrimitive || !field.isString)
                return FraplinError.unprocessable("frappe cloud response json has no field '$key' of type String").err
            return Success(field.content)
        }
    }

}
