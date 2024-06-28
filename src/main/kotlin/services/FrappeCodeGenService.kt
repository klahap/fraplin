package io.github.klahap.fraplin.services

import io.github.klahap.fraplin.models.DocField
import io.github.klahap.fraplin.models.DocTypeInfo
import io.github.klahap.fraplin.util.*
import kotlinx.coroutines.flow.toList
import java.nio.file.Path
import kotlin.io.path.*


class FrappeCodeGenService(
    private val client: FrappeSiteService,
) {

    suspend fun generate(packageName: String, output: Path, docTypeInfos: Set<DocTypeInfo>) {
        val docTypeNames = docTypeInfos.map { it.name }.toSet()
        val allDocTypes = client.getDocTypes(docTypeInfos).toList().associateBy { it.docTypeName }
        if (!allDocTypes.keys.containsAll(docTypeNames))
            throw Exception("doc types not found: ${docTypeNames - allDocTypes.keys}")

        val baseDocTypes = allDocTypes.filterKeys { docTypeNames.contains(it) }.values.toList()
        val childDocTypes = baseDocTypes.asSequence().flatMap { it.fields }
            .filterIsInstance<DocField.Table>()
            .map { it.option }.toSet()
            .let { it - baseDocTypes.map { d -> d.docTypeName }.toSet() }
            .map { allDocTypes[it]!! }

        val docTypesGen = baseDocTypes + childDocTypes
        val docTypesGenNames = docTypesGen.map { it.docTypeName }.toSet()
        val docTypeDummyNames = docTypesGen.asSequence()
            .flatMap { it.fields }
            .filterIsInstance<DocField.Link>()
            .map { it.option }
            .filter { !docTypesGenNames.contains(it) }
            .toSet()
        val dummyDocTypes = allDocTypes
            .filterKeys { docTypeDummyNames.contains(it) }.values
            .map { it.copy(module = null) }
            .sortedBy { it.docTypeName }

        val context = CodeGenContext(
            packageName = packageName,
            docTypes = (docTypesGen + dummyDocTypes).associateBy { it.docTypeName },
        )

        val outputPath = output.toAbsolutePath().apply {
            if (isDirectory())
                @OptIn(ExperimentalPathApi::class) deleteRecursively()
            createDirectories()
        }

        PathUtil.defaultCodeFiles.forEach {
            outputPath.resolve(it.relativePath)
                .apply { parent.createDirectories() }
                .writeText(it.getContent(packageName))
            println("created: ${it.relativePath}")
        }

        docTypesGen.map {
            val relativePath = "doctype/${it.prettyModule}/${it.prettyName}.kt"
            outputPath.resolve(relativePath)
                .apply { parent.createDirectories() }
                .writeText(it.getCode(context).toString())
            println("created: $relativePath")
        }

        fileBuilder(
            packageName = "${context.packageName}.doctype",
            fileName = "Helper.kt",
        ) {
            docTypesGen.forEach { it.addHelperCode(context) }
        }.let { outputPath.resolve("doctype/Helper.kt").writeText(it.toString()) }


        fileBuilder(
            packageName = "${context.packageName}.doctype",
            fileName = "Dummy.kt",
        ) {
            dummyDocTypes.forEach { docType -> docType.addDummyCode(context) }
        }.let { outputPath.resolve("doctype/Dummy.kt").writeText(it.toString()) }

    }
}