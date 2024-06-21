package default_code.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName
import kotlin.reflect.KProperty1

fun Boolean.toFrappeFilterValue() = FrappeFilter.Value(if (this) "true" else "false")

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Boolean?>.eq(value: Boolean) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, value.toFrappeFilterValue()))
