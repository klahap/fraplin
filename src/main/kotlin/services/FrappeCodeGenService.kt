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
        val allDocTypeInfo = docTypeInfos + setOf(DocTypeInfo(name = "User"))
        val docTypeNames = allDocTypeInfo.map { it.name }.toSet()
        val allDocTypes = client.getDocTypes(allDocTypeInfo).toList().associateBy { it.docTypeName }
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
            outputPath = output.toAbsolutePath(),
            docTypes = (docTypesGen + dummyDocTypes).associateBy { it.docTypeName },
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
                .writeText(it.getContent(packageName))
            println("created: ${it.relativePath}")
        }

        docTypesGen.map {
            it.buildFile(context)
            println("created: ${it.relativePath}")
        }

        fileBuilder(
            packageName = "${context.packageName}.doctype",
            filePath = context.outputPath.resolve("doctype/_HelperDocTypes.kt"),
        ) {
            docTypesGen.forEach { it.addHelperCode(context) }
        }

        fileBuilder(
            packageName = "${context.packageName}.doctype",
            filePath = context.outputPath.resolve("doctype/_DummyDocTypes.kt"),
        ) {
            dummyDocTypes.forEach { docType -> docType.addDummyCode(context) }
        }
    }
}