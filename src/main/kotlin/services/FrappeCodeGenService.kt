package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.*
import io.github.klahap.fraplin.models.DocType.Companion.addDummyCode
import io.github.klahap.fraplin.models.DocType.Companion.addHelperCode
import io.github.klahap.fraplin.models.DocType.Companion.buildFile
import io.github.klahap.fraplin.models.DocType.Companion.toOpenApiComponents
import io.github.klahap.fraplin.models.DocType.Companion.toOpenApiPaths
import io.github.klahap.fraplin.models.WhiteListFunction.Companion.addWhiteListFunction
import io.github.klahap.fraplin.models.config.FraplinOutputConfig
import io.github.klahap.fraplin.models.openapi.OpenApiSpec
import io.github.klahap.fraplin.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.*


class FrappeCodeGenService(
    @OptIn(ExperimentalSerializationApi::class)
    val json: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }
) {

    suspend fun generate(config: FraplinOutputConfig, spec: FraplinSpec) {

        val context = CodeGenContext(
            config = config,
            docTypes = sequenceOf(
                spec.docTypes,
                spec.openApi.flatMap { it.docTypes },
                spec.dummyDocTypes,
            ).flatten().associateBy { it.docTypeName },
        )

        val synchronizer = DirectorySynchronizer(context.outputPath)

        PathUtil.defaultCodeFiles.forEach {
            synchronizer.sync(
                relativePath = it.relativePath,
                content = it.getContent(context.packageName)
            )
        }

        spec.docTypes.map {
            synchronizer.sync(
                relativePath = it.relativePath,
                content = it.buildFile(context),
            )
        }

        synchronizer.sync(
            packageName = "${context.packageName}.doctype",
            relativePath = "doctype/_HelperDocTypes.kt",
        ) {
            spec.docTypes.forEach { it.addHelperCode(context) }
        }

        synchronizer.sync(
            packageName = "${context.packageName}.doctype",
            relativePath = "doctype/_DummyDocTypes.kt",
        ) {
            spec.dummyDocTypes.forEach { docType -> docType.addDummyCode(context) }
        }

        synchronizer.sync(
            packageName = context.packageName,
            relativePath = "WhiteListFun.kt",
        ) {
            addWhiteListFunction(packageName = context.packageName, functions = spec.whiteListFunctions)
        }
        synchronizer.cleanup()

        spec.openApi.forEach { generateOpenApiSpec(config = config, spec = it) }
    }

    private fun generateOpenApiSpec(
        config: FraplinOutputConfig,
        spec: FraplinOpenApiSpec,
    ) {
        if (spec.docTypes.isEmpty()) return
        if (config.openApiPath == null) throw Exception("no output path for OpenApi specs defined")
        config.openApiPath.createDirectories()

        val context = OpenApiGenContext(
            pathPrefix = spec.pathPrefix,
            schemaPrefix = spec.schemaPrefix,
            tags = spec.pathTags,
        )
        val openApiSpec = OpenApiSpec.openApiSpec(title = spec.name, version = spec.version) {
            addComponent(DocField.DocStatus.getOpenApiSpecEnum(context))

            spec.docTypes.forEach { docType ->
                addComponents(docType.toOpenApiComponents(context))
                addPaths(docType.toOpenApiPaths(context))
            }
        }
        val openApiSpecStr = json.encodeToString(openApiSpec.toJson())

        val syncType = DirectorySynchronizer.sync(
            path = config.openApiPath.resolve("oas-${spec.name.toHyphenated()}.json"),
            content = openApiSpecStr
        )
        println("openapi spec '${spec.name}' ${syncType.name.lowercase()}")
    }
}