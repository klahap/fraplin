package io.github.klahap.fraplin

import io.github.klahap.fraplin.models.DocTypeInfo
import io.github.klahap.fraplin.services.FrappeCodeGenService
import io.github.klahap.fraplin.services.FrappeCloudBaseService
import io.github.klahap.fraplin.services.FrappeSiteService
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import org.gradle.api.Project
import java.nio.file.Path
import kotlin.io.path.*


class Plugin : org.gradle.api.Plugin<Project> {

    override fun apply(project: Project) {
        val extensionFrappe = project.extensions.create(
            "frappeDslGenerator",
            FrappeDslGeneratorExtension::class.java,
        )

        project.task("generateFrappeDsl") {
            doLast {
                val config = extensionFrappe.toValid()
                runBlocking {
                    val frappeSiteClient = config.createClient()
                    val frappeCodeGenService = FrappeCodeGenService(frappeSiteClient)
                    frappeCodeGenService.generate(
                        packageName = config.packageName,
                        output = config.output,
                        docTypeInfos = config.docTypes,
                    )
                }
            }
        }
        project.task("cleanFrappeDsl") {
            doLast {
                @OptIn(ExperimentalPathApi::class) extensionFrappe.toValid().output.deleteRecursively()
            }
        }
    }
}

open class FrappeDslGeneratorExtension {
    var site: SiteConfig? = null
    var output: String? = null
    var packageName: String? = null
    var docTypes: Set<DocTypeInfo> = setOf()

    sealed interface SiteConfig {
        data class Site(
            val url: HttpUrl,
            val userToken: String,
        ) : SiteConfig

        data class Cloud(
            val url: HttpUrl,
            val cloudToken: String,
        ) : SiteConfig
    }
}

data class FrappeDslGeneratorExtensionValid(
    val site: FrappeDslGeneratorExtension.SiteConfig,
    val output: Path,
    val packageName: String,
    val docTypes: Set<DocTypeInfo>,
) {
    suspend fun createClient() = when (site) {
        is FrappeDslGeneratorExtension.SiteConfig.Cloud ->
            FrappeCloudBaseService(token = site.cloudToken).getSiteClient(site.url)

        is FrappeDslGeneratorExtension.SiteConfig.Site ->
            FrappeSiteService(siteUrl = site.url, userApiToken = site.userToken)
    }
}

fun FrappeDslGeneratorExtension.toValid(): FrappeDslGeneratorExtensionValid {
    return FrappeDslGeneratorExtensionValid(
        site = site ?: throw Exception("no site defined"),
        output = output?.let { Path(it) } ?: throw Exception("no output path defined"),
        packageName = packageName ?: throw Exception("no output package name defined"),
        docTypes = docTypes.toSet().takeIf { it.isNotEmpty() } ?: throw Exception("no DocTypes defined"),
    )
}


