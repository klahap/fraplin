package io.github.klahap.fraplin.models.openapi


data class Path(
    val route: String,
    val tags: Set<String>,
    val operationId: String,
    val parameters: List<Parameter>,
    val response: Response,
) {
    fun toJson() = route to jsonOf(
        "get" to jsonOf(
            tags.takeIf { it.isNotEmpty() }?.let { t -> "tags" to jsonOf(t.map { jsonOf(it) }) },
            "operationId" to jsonOf(operationId),
            parameters.takeIf { it.isNotEmpty() }?.let { p -> "parameters" to jsonOf(p.map { it.toJson() }) },
            "responses" to jsonOf(
                response.toJson()
            ),
        )
    )

    data class Parameter(
        val name: String,
        val source: Source,
        val required: Boolean,
        val schema: Schema
    ) {
        fun toJson() = jsonOf(
            "name" to jsonOf(name),
            "in" to jsonOf(source.value),
            "required" to jsonOf(required),
            "schema" to schema.toJson(),
        )

        enum class Source(val value: String) {
            QUERY("query"),
            PATH("path"),
            HEADER("header"),
        }
    }

    data class Response(
        val status: Int = 200,
        val description: String,
        val schema: Schema,
    ) {
        fun toJson() = "$status" to jsonOf(
            "description" to jsonOf(description),
            "content" to jsonOf(
                "application/json" to jsonOf(
                    "schema" to schema.toJson()
                )
            )
        )
    }
}
