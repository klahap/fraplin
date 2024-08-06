package io.github.klahap.fraplin

import io.github.klahap.fraplin.services.FrappeCodeGenService
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.system.exitProcess

suspend fun generate(configPath: Path) {
    if (!configPath.exists()) throw FileNotFoundException("file $configPath not found")
    val config = Json.decodeFromString<FrappeDslGeneratorExtensionValid>(configPath.readText())
    val frappeSiteClient = config.createClient()
    val frappeCodeGenService = FrappeCodeGenService(frappeSiteClient)
    frappeCodeGenService.generate(
        packageName = config.packageName,
        output = config.output,
        docTypeInfos = config.docTypes,
    )
    exitProcess(0)
}

suspend fun main(args: Array<String>) {
    if (args.size == 2 && args.first() == "generate") {
        generate(Path(args.last()))
    } else {
        println("invalid args")
        exitProcess(1)
    }
}
