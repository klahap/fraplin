package io.github.klahap.fraplin.models.config

import io.github.klahap.fraplin.util.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class FraplinConfig(
    @Serializable(PathSerializer::class) val specFile: Path,
    val input: FraplinInputConfig,
    val output: FraplinOutputConfig,
) {
    open class Builder(
        var specFile: Path? = null,
        var input: FraplinInputConfig.Builder = FraplinInputConfig.Builder(),
        var output: FraplinOutputConfig.Builder = FraplinOutputConfig.Builder(),
    ) {
        fun buildSpecFile() = specFile ?: throw Exception("no fraplin spec file defined")
        fun buildInput() = input.build()
        fun buildOutput() = output.build()
        fun build() = FraplinConfig(
            specFile = buildSpecFile(),
            input = buildInput(),
            output = buildOutput(),
        )

        fun input(block: FraplinInputConfig.Builder.() -> Unit) = input.apply(block)
        fun output(block: FraplinOutputConfig.Builder.() -> Unit) = output.apply(block)
    }
}