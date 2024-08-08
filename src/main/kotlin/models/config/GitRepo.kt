package io.github.klahap.fraplin.models.config

import java.nio.file.Path


sealed interface GitRepo {
    val appName: String

    data class GitHub(
        val owner: String,
        val repo: String,
        val version: String? = null,
        val creds: Credentials? = null,
        override val appName: String = repo,
    ) : GitRepo {
        val cloneUrl
            get(): String {
                val tokenStr = creds?.let { "$it@" } ?: ""
                return "https://${tokenStr}github.com/$owner/$repo.git"
            }
        val url get() = "https://github.com/$owner/$repo.git"

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
        ) {
            fun creds(block: Credentials.Builder.() -> Unit) {
                creds = Credentials.Builder().apply(block).build()
            }

            fun build() = GitHub(
                owner = owner ?: throw Exception("no owner for github repo defined"),
                repo = repo ?: throw Exception("no repo for github repo defined"),
                version = version,
                creds = creds,
                appName = appName ?: repo!!
            )
        }
    }


    data class Local(
        val path: Path,
        override val appName: String,
    ) : GitRepo {
        data class Builder(
            var path: Path? = null,
            var appName: String? = null,
        ) {
            fun build() = Local(
                path = path ?: throw Exception("no path for local git repo defined"),
                appName = appName ?: throw Exception("no app-name for local git repo defined"),
            )
        }
    }
}
