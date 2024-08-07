package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.DocType.Companion.addDummyCode
import io.github.klahap.fraplin.models.DocType.Companion.addHelperCode
import io.github.klahap.fraplin.models.DocType.Companion.buildFile
import io.github.klahap.fraplin.models.FraplinSpec
import io.github.klahap.fraplin.models.config.FraplinOutputConfig
import io.github.klahap.fraplin.util.*
import kotlin.io.path.*


class FrappeCodeGenService {

    suspend fun generate(config: FraplinOutputConfig, spec: FraplinSpec) {
        val context = CodeGenContext(
            config = config,
            docTypes = (spec.docTypes + spec.dummyDocTypes).associateBy { it.docTypeName },
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
    }
}