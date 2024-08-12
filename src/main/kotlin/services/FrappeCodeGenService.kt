package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.CodeGenContext
import io.github.klahap.fraplin.models.DocField
import io.github.klahap.fraplin.models.DocType.Companion.addDummyCode
import io.github.klahap.fraplin.models.DocType.Companion.addHelperCode
import io.github.klahap.fraplin.models.DocType.Companion.buildFile
import io.github.klahap.fraplin.models.DocType.Companion.toOpenApiComponents
import io.github.klahap.fraplin.models.DocType.Companion.toOpenApiPaths
import io.github.klahap.fraplin.models.FraplinSpec
import io.github.klahap.fraplin.models.OpenApiGenContext
import io.github.klahap.fraplin.models.config.FraplinOpenApiConfig
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
            docTypes = (spec.docTypes + spec.virtualDocTypes + spec.dummyDocTypes).associateBy { it.docTypeName },
        )

        context.outputPath.apply {
            if (isDirectory())
                @OptIn(ExperimentalPathApi::class) deleteRecursively()
            createDirectories()
            resolve("doctype").createDirectories()
        }

        PathUtil.defaultCodeFiles.forEach {
            context.outputPath.resolve(it.relativePath)
                .apply { parent.createDirectories() }
                .writeText(it.getContent(context.packageName))
            println("created: ${it.relativePath}")
        }

        spec.docTypes.map {
            it.buildFile(context)
            println("created: ${it.relativePath}")
        }

        fileBuilder(
            packageName = "${context.packageName}.doctype",
            filePath = context.outputPath.resolve("doctype/_HelperDocTypes.kt"),
        ) {
            spec.docTypes.forEach { it.addHelperCode(context) }
        }

        fileBuilder(
            packageName = "${context.packageName}.doctype",
            filePath = context.outputPath.resolve("doctype/_DummyDocTypes.kt"),
        ) {
            spec.dummyDocTypes.forEach { docType -> docType.addDummyCode(context) }
        }

        if (config.openapi != null)
            generateOpenApiSpec(config.openapi, spec = spec)
    }

    private fun generateOpenApiSpec(
        config: FraplinOpenApiConfig,
        spec: FraplinSpec,
    ) {
        if (spec.virtualDocTypes.isEmpty()) return
        config.path.parent.createDirectories()

        val context = OpenApiGenContext(
            pathPrefix = config.pathPrefix,
            schemaPrefix = config.schemaPrefix,
            tags = config.pathTags,
        )
        val openApiSpec = OpenApiSpec.openApiSpec(title = config.title, version = config.version) {
            addComponent(DocField.DocStatus.getOpenApiSpecEnum(context))

            spec.virtualDocTypes.forEach { docType ->
                addComponents(docType.toOpenApiComponents(context))
                addPaths(docType.toOpenApiPaths(context))
            }
        }
        val openApiSpecStr = json.encodeToString(openApiSpec.toJson())
        config.path.writeText(openApiSpecStr)
    }
}