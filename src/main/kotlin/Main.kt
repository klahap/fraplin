package io.github.klahap.fraplin

import io.github.klahap.fraplin.services.FraplinService
import kotlin.io.path.*
import kotlin.system.exitProcess


suspend fun main(args: Array<String>) {
    val service = FraplinService()
    if (args.size == 2 && args.first() == "generateDsl") {
        val config = service.readConfig(Path(args.last()))
        val spec = service.readSpec(config.specFile)
        service.generateDsl(config = config.output, spec = spec)
    } else if (args.size == 2 && args.first() == "generateSpec") {
        val config = service.readConfig(Path(args.last()))
        val spec = service.generateSpec(config.input)
        service.writeSpec(spec = spec, path = config.specFile)
    } else if (args.size == 2 && args.first() == "generate") {
        val config = service.readConfig(Path(args.last()))
        val spec = service.generateSpec(config.input)
        service.writeSpec(spec = spec, path = config.specFile)
        service.generateDsl(config = config.output, spec = spec)
    } else {
        println("invalid args")
        exitProcess(1)
    }
    exitProcess(0)
}
