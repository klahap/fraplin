package default_code.model.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName
import kotlin.reflect.KProperty1


@JvmInline
value class FrappeFilterBoolean(val data: Boolean) : FrappeFilterValue {
    override fun serialize() = if (data) "true" else "false"
}

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Boolean?>.eq(value: Boolean) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, FrappeFilterBoolean(value)))
