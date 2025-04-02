package default_code.service

import default_code.FraplinException
import default_code.FraplinResult
import default_code.util.*
import io.github.goquati.kotlin.util.Success
import io.github.goquati.kotlin.util.flatMap
import io.github.goquati.kotlin.util.getOr
import io.github.goquati.kotlin.util.isSuccess
import io.github.goquati.kotlin.util.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
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
                team?.takeIf { it.isNotBlank() }?.let {
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

    suspend fun existsSite(siteUrl: HttpUrl): Boolean = getSite(siteUrl).isSuccess()

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
                }.getOr { return@getValueForKey it.result }
            SiteToken(
                token = token,
                expiresIn = now.plusHours(3 * 24 - 1),
            ).let { Success(it) }
        }.getOr {
            siteTokens.remove(siteUrl)
            return it.result
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
                ?: return FraplinException.unprocessable("frappe cloud response json has no field '$key'").result
            if (field !is JsonPrimitive || !field.isString)
                return FraplinException.unprocessable("frappe cloud response json has no field '$key' of type String").result
            return Success(field.content)
        }

        private suspend fun <K, V> ConcurrentHashMap<K, CompletableFuture<V>>.getValueForKey(
            key: K,
            block: suspend (K) -> V,
        ): V = withContext(Dispatchers.IO) {
            val scope = this
            computeIfAbsent(key) {
                scope.future { block(key) }
            }.await()
        }
    }

}
