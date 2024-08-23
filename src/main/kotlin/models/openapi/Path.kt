package io.github.klahap.fraplin.models.openapi

data class Path(
    val route: String,
    val endpoints: List<Endpoint>,
) {
    fun toJson() = route to jsonOf(
        endpoints.associate { it.toJson() }
    )
}