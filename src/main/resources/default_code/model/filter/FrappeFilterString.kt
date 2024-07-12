package default_code.model.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName
import kotlin.reflect.KProperty1


@JvmInline
value class FrappeFilterString(val data: String) : FrappeFilterValue {
    override fun serialize() = "\"$data\""
}

@JvmInline
value class FrappeFilterStringSet(val data: Set<String>) : FrappeFilterValue {
    override fun serialize() = data.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }
}

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, String?>.eq(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, FrappeFilterString(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, String?>.notEq(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotEq, FrappeFilterString(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("inIterableString")
infix fun <T> KProperty1<T, String?>.In(values: Iterable<String>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.In, FrappeFilterStringSet(values.toSet())))

context(FrappeFilterSet.Builder<T>)
@JvmName("notInIterableString")
infix fun <T> KProperty1<T, String?>.notIn(values: Iterable<String>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotIn, FrappeFilterStringSet(values.toSet())))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, String?>.like(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Like, FrappeFilterString(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, String?>.notLike(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotLike, FrappeFilterString(value)))
