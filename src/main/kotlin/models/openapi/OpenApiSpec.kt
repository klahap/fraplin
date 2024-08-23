package io.github.klahap.fraplin.models.openapi

data class OpenApiSpec(
    val info: Info,
    val paths: List<Path>,
    val components: List<Component>,
) {
    fun toJson() = jsonOf(
        "openapi" to jsonOf("3.1.0"),
        "info" to info.toJson(),
        "paths" to jsonOf(paths.associate { it.toJson() }),
        "components" to jsonOf(
            "schemas" to jsonOf(components.associate { it.toJson() })
        )
    )

    data class Info(
        val title: String,
        val version: String,
    ) {
        fun toJson() = jsonOf(
            "title" to jsonOf(title),
            "version" to jsonOf(version),
        )
    }

    class Builder(
        private val info: Info,
    ) {
        private val paths: MutableList<Path> = mutableListOf()
        private val components: MutableList<Component> = mutableListOf()

        fun addPath(path: Path) = paths.add(path)
        fun addPaths(path: Iterable<Path>) = paths.addAll(path)
        fun addComponent(component: Component) = components.add(component)
        fun addComponents(component: Iterable<Component>) = components.addAll(component)

        fun build(): OpenApiSpec {
            check()
            return OpenApiSpec(
                info = info,
                paths = paths.toList(),
                components = components.toList(),
            )
        }

        private fun check() {
            val required = sequenceOf(
                paths.map { it.response.schema },
                paths.flatMap { p -> p.parameters.map { it.schema } },
                components.filterIsInstance<Component.Object>().flatMap { c -> c.properties.map { it.schema } },
            ).flatten().mapNotNull {
                when (it) {
                    is Schema.ArrayRef -> it.value
                    is Schema.Ref -> it.value
                    is Schema.Primitive, null -> null
                }
            }.toSet()
            val actual = components.map { it.toRef() }.toSet()
            val missing = required - actual
            if (missing.isNotEmpty())
                throw Exception("error creating OpenApi spec, missing components: $missing")
        }
    }

    companion object {
        fun openApiSpec(
            title: String,
            version: String,
            block: Builder.() -> Unit,
        ) = Builder(Info(title = title, version = version)).apply(block).build()
    }
}
