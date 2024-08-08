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
    ) : FraplinSourceConfig {
        data class Builder(
            var url: HttpUrl? = null,
            var userToken: String? = null,
        ) {
            fun build() = Site(
                url = url ?: throw Exception("frappe site url not defined"),
                userToken = userToken ?: throw Exception("frappe user token not defined"),
            )
        }
    }

    @Serializable
    @SerialName("cloud")
    data class Cloud(
        @Serializable(HttpUrlSerializer::class) val url: HttpUrl,
        val cloudToken: String,
        val team: String?,
    ) : FraplinSourceConfig {
        data class Builder(
            var url: HttpUrl? = null,
            var cloudToken: String? = null,
            var team: String? = null,
        ) {
            fun build() = Cloud(
                url = url ?: throw Exception("frappe site url not defined"),
                cloudToken = cloudToken ?: throw Exception("frappe cloud token not defined"),
                team = team,
            )
        }
    }

    @Serializable
    @SerialName("repos")
    data class Repos(
        val repos: Set<GitRepo>,
    ) : FraplinSourceConfig {
        data class Builder(
            val repos: MutableSet<GitRepo> = mutableSetOf()
        ) {
            fun gitHub(block: GitRepo.GitHub.Builder.() -> Unit) =
                repos.add(GitRepo.GitHub.Builder().apply(block).build())

            fun local(block: GitRepo.Local.Builder.() -> Unit) =
                repos.add(GitRepo.Local.Builder().apply(block).build())

            fun build() = Repos(
                repos = repos.toSet().takeIf { it.isNotEmpty() } ?: throw Exception("no frappe git repos defined")
            )
        }
    }
}