package default_code.model

import default_code.DocType
import default_code.DocTypeAbility


class FrappeFilterSet(
    val filters: Set<FrappeFilter>,
) {
    constructor(vararg filters: FrappeFilter) : this(filters.toSet())

    fun serialize() = filters.joinToString(separator = ",", prefix = "[", postfix = "]") { it.serialize() }

    class Builder<T> where T : DocType, T : DocTypeAbility.Query {
        private val filters: MutableList<FrappeFilter> = mutableListOf()

        fun add(value: FrappeFilter) = also {
            val filterAlreadyExists = filters.any { it.fieldName == value.fieldName && it.operator == value.operator }
            if (filterAlreadyExists)
                throw Exception("filter '${value.operator.name}' already exists for field '${value.fieldName}'")
            filters.add(value)
        }

        fun build() = FrappeFilterSet(filters = filters.toSet())
    }
}