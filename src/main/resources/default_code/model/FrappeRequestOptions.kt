package default_code.model

import default_code.DocType
import default_code.DocTypeAbility
import default_code.filter.toFrappeFilterValue
import default_code.util.frappeName
import kotlin.reflect.KProperty1

data class FrappeRequestOptions(
    val filters: FrappeFilterSet? = null,
    val parent: FrappeDocTypeName? = null,
    val orderBy: FrappeOrderBy? = null,
) {

    class Builder<T> where T : DocType, T : DocTypeAbility.Query {
        private var filtersSet: FrappeFilterSet? = null
        private var orderBy: FrappeOrderBy? = null

        fun filters(
            block: FrappeFilterSet.Builder<T>.() -> Unit,
        ) = FrappeFilterSet.Builder<T>().apply(block).build().also { filtersSet = it }

        fun orderByAsc(field: KProperty1<T, *>) = also {
            orderBy = FrappeOrderBy(fieldName = field.frappeName, direction = FrappeOrderBy.Direction.Asc)
        }

        fun orderByDesc(field: KProperty1<T, *>) = also {
            orderBy = FrappeOrderBy(fieldName = field.frappeName, direction = FrappeOrderBy.Direction.Desc)
        }

        fun build() = FrappeRequestOptions(
            filters = filtersSet,
            orderBy = orderBy,
        )

    }
}