package default_code.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName
import kotlin.reflect.KProperty1


fun String.toFrappeFilterValue() = FrappeFilter.Value("\"$this\"")

@JvmName("toFilterValueString")
fun Iterable<String>.toFrappeFilterValue() =
    FrappeFilter.Value(toSet().joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" })


context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, String?>.eq(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, String?>.notEq(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotEq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("inIterableString")
infix fun <T> KProperty1<T, String?>.In(values: Iterable<String>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.In, values.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("notInIterableString")
infix fun <T> KProperty1<T, String?>.notIn(values: Iterable<String>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotIn, values.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, String?>.like(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Like, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, String?>.notLike(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotLike, value.toFrappeFilterValue()))
