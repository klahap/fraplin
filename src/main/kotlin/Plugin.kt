package io.github.klahap.fraplin

import io.github.klahap.fraplin.models.DocTypeInfo
import io.github.klahap.fraplin.services.FrappeCodeGenService
import io.github.klahap.fraplin.services.FrappeCloudBaseService
import io.github.klahap.fraplin.services.FrappeSiteService
import io.github.klahap.fraplin.util.HttpUrlSerializer
import io.github.klahap.fraplin.util.PathSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import org.gradle.api.Project
import java.nio.file.Path
import kotlin.io.path.*


class Plugin : org.gradle.api.Plugin<Project> {

    private val json = Json {
        prettyPrint = true
    }

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
        project.task("printFrappeDslGenerationConfig") {
            doLast {
                val config = extensionFrappe.toValid().let {
                    it.copy(output = it.output.absolute())
                }
                println()
                println(json.encodeToString(config))
            }
        }
    }
}

fun readVariableFromFile(file: String, regex: Regex): MatchResult {
    val path = Path(file).takeIf { it.exists() } ?: throw Exception("file not exists: $file")
    return regex.findAll(path.readText()).toList()
        .let {
            if (it.size > 1) throw Exception("multiple results for '${regex.pattern}' in file $file found")
            if (it.isEmpty()) throw Exception("no results for '${regex.pattern}' in file $file found")
            it.single()
        }
}

open class FrappeDslGeneratorExtension {
    var site: SiteConfig? = null
    var output: String? = null
    var packageName: String? = null
    var docTypes: Set<DocTypeInfo> = setOf()

    @Serializable
    sealed interface SiteConfig {
        @Serializable
        @SerialName("site")
        data class Site(
            @Serializable(HttpUrlSerializer::class) val url: HttpUrl,
            val userToken: String,
        ) : SiteConfig

        @Serializable
        @SerialName("cloud")
        data class Cloud(
            @Serializable(HttpUrlSerializer::class) val url: HttpUrl,
            val cloudToken: String,
        ) : SiteConfig
    }
}

@Serializable
data class FrappeDslGeneratorExtensionValid(
    val site: FrappeDslGeneratorExtension.SiteConfig,
    @Serializable(PathSerializer::class) val output: Path,
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


