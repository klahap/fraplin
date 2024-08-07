package io.github.klahap.fraplin.models.config

import io.github.klahap.fraplin.util.HttpUrlSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl


@Serializable
sealed interface FraplinSourceConfig {
    @Serializable
    @SerialName("site")
    data class Site(
        @Serializable(HttpUrlSerializer::class) val url: HttpUrl,
        val userToken: String,
    ) : FraplinSourceConfig

    @Serializable
    @SerialName("cloud")
    data class Cloud(
        @Serializable(HttpUrlSerializer::class) val url: HttpUrl,
        val cloudToken: String,
        val team: String? = null,
    ) : FraplinSourceConfig
}