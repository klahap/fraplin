package io.github.klahap.fraplin

import io.github.klahap.fraplin.models.config.FraplinConfig
import io.github.klahap.fraplin.services.FraplinService
import io.github.klahap.fraplin.services.FrappeCodeGenService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.Project

class Plugin : org.gradle.api.Plugin<Project> {
    private val json = Json {
        prettyPrint = true
    }

    override fun apply(project: Project) {
        val fraplin = project.extensions.create("fraplin", FraplinConfig.Builder::class.java)
        val service = FraplinService()

        fun generateFraplinSpec() {
            val specFile = fraplin.buildSpecFile()
            val config = fraplin.buildInput()
            runBlocking {
                val spec = service.generateSpec(config)
                service.writeSpec(spec = spec, path = specFile)
            }
        }

        fun generateFraplinDsl() {
            val specFile = fraplin.buildSpecFile()
            val config = fraplin.buildOutput()
            runBlocking {
                val spec = service.readSpec(specFile)
                FrappeCodeGenService().generate(config = config, spec = spec)
            }
        }

        project.task("generateFraplinDsl") { task ->
            task.group = TASK_GROUP
            task.doLast { generateFraplinDsl() }
        }
        project.task("generateFraplinSpec") { task ->
            task.group = TASK_GROUP
            task.doLast { generateFraplinSpec() }
        }
        project.task("generateFraplin") { task ->
            task.group = TASK_GROUP
            task.doLast {
                generateFraplinSpec()
                generateFraplinDsl()
            }
        }
        project.task("printFraplinConfig") { task ->
            task.group = TASK_GROUP
            task.doLast {
                val config = fraplin.build()
                println()
                println(json.encodeToString(config))
            }
        }
    }

    companion object {
        private const val TASK_GROUP = "fraplin tools"
    }
}
