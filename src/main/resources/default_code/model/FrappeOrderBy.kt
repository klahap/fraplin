package default_code.model


data class FrappeOrderBy(
    val fieldName: String,
    val direction: Direction,
) {
    fun serialize() = "$fieldName ${direction.value}"

    enum class Direction(val value: String) {
        Asc("asc"),
        Desc("desc"),
    }
}