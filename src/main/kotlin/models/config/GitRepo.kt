package io.github.klahap.fraplin.models.config

import io.github.klahap.fraplin.util.PathSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path


@Serializable
sealed interface GitRepo {
    val appName: String
    val addWhiteListedFunctions: Boolean

    @Serializable
    @SerialName("github")
    data class GitHub(
        val owner: String,
        val repo: String,
        val version: String? = null,
        val creds: Credentials? = null,
        override val appName: String = repo,
        override val addWhiteListedFunctions: Boolean = ADD_WHITELISTED_FUNCTIONS_DEFAULT,
    ) : GitRepo {
        val cloneUrl
            get(): String {
                val tokenStr = creds?.let { "$it@" } ?: ""
                return "https://${tokenStr}github.com/$owner/$repo.git"
            }
        val url get() = "https://github.com/$owner/$repo.git"

        @Serializable
        data class Credentials(
            val username: String,
            val token: String,
        ) {
            override fun toString() = "$username:$token"
            data class Builder(
                var username: String? = null,
                var token: String? = null,
            ) {
                fun build() = Credentials(
                    username = username ?: throw Exception("no username for github credentials defined"),
                    token = token ?: throw Exception("no token for github credentials defined"),
                )
            }
        }

        data class Builder(
            var owner: String? = null,
            var repo: String? = null,
            var version: String? = null,
            private var creds: Credentials? = null,
            var appName: String? = null,
            var addWhiteListedFunctions: Boolean = ADD_WHITELISTED_FUNCTIONS_DEFAULT,
        ) {
            fun creds(block: Credentials.Builder.() -> Unit) {
                creds = Credentials.Builder().apply(block).build()
            }

            fun build() = GitHub(
                owner = owner ?: throw Exception("no owner for github repo defined"),
                repo = repo ?: throw Exception("no repo for github repo defined"),
                version = version,
                creds = creds,
                appName = appName ?: repo!!,
                addWhiteListedFunctions = addWhiteListedFunctions,
            )
        }
    }


    @Serializable
    @SerialName("local")
    data class Local(
        @Serializable(PathSerializer::class) val path: Path,
        override val appName: String,
        override val addWhiteListedFunctions: Boolean = ADD_WHITELISTED_FUNCTIONS_DEFAULT,
    ) : GitRepo {
        val appPath: Path get() = path.resolve(appName)

        data class Builder(
            var path: Path? = null,
            var appName: String? = null,
            var addWhiteListedFunctions: Boolean = ADD_WHITELISTED_FUNCTIONS_DEFAULT,
        ) {
            fun build() = Local(
                path = path ?: throw Exception("no path for local git repo defined"),
                appName = appName ?: throw Exception("no app-name for local git repo defined"),
                addWhiteListedFunctions = addWhiteListedFunctions,
            )
        }
    }

    companion object {
        private const val ADD_WHITELISTED_FUNCTIONS_DEFAULT = false
    }
}
