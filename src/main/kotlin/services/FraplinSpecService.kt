package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.config.FraplinInputConfig
import io.github.klahap.fraplin.models.config.FraplinSourceConfig
import okhttp3.OkHttpClient

class FraplinSpecService(
    private val client: OkHttpClient,
) {
    suspend fun getSpec(config: FraplinInputConfig) = when (config.source) {
        is FraplinSourceConfig.Cloud -> config.source.getFrappeSite().getFraplinSpec(config.docTypes)
        is FraplinSourceConfig.Site -> config.source.getFrappeSite().getFraplinSpec(config.docTypes)
    }

    private suspend fun FraplinSourceConfig.Cloud.getFrappeSite() =
        FrappeCloudBaseService(token = cloudToken, team = team, client = client).getSiteClient(url)

    private fun FraplinSourceConfig.Site.getFrappeSite() =
        FrappeSiteService(siteUrl = url, userApiToken = userToken, client = client)
}