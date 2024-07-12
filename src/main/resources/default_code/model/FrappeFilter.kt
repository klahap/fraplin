package default_code.model

import default_code.model.filter.FrappeFilterValue


data class FrappeFilter(
    val fieldName: String,
    val operator: Operator,
    val value: FrappeFilterValue,
) {
    fun serialize() = """["$fieldName","${operator.value}",${value.serialize()}]"""

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
