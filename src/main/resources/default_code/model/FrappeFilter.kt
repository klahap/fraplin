package default_code.model


data class FrappeFilter(
    val fieldName: String,
    val operator: Operator,
    val value: Value,
) {
    fun serialize() = """["$fieldName","${operator.value}",${value.data}]"""

    @JvmInline
    value class Value(val data: String)

    enum class Operator(val value: String) {
        Eq("="),
        In("in"),
        Like("like"),
        Between("between"),

        NotEq("!="),
        NotIn("not in"),
        NotLike("not like"),
        NotBetween("not between"),

        Gt(">"),
        Gte(">="),
        Lt("<"),
        Lte("<="),

        After(">"),
        Before("<"),
    }
}
