package default_code.model.filter

import kotlin.reflect.KProperty1
import default_code.DocType
import default_code.DocTypeAbility
import default_code.FrappeEnum
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName

fun <T> T.toFrappeFilterValue() where T : Enum<T>, T : FrappeEnum<T> = FrappeFilterString(origin)

@JvmName("toFilterValueEnum")
fun <T> Iterable<T>.toFrappeFilterValue() where T : Enum<T>, T : FrappeEnum<T> =
    FrappeFilterStringSet(map { it.origin }.toSet())

context(FrappeFilterSet.Builder<T>)
infix fun <T, E> KProperty1<T, E?>.eq(value: E) where T : DocType, T : DocTypeAbility.Query, E : Enum<E>, E : FrappeEnum<E> =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T, E> KProperty1<T, E?>.notEq(value: E) where T : DocType, T : DocTypeAbility.Query, E : Enum<E>, E : FrappeEnum<E> =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotEq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("inIterableEnum")
infix fun <T, E> KProperty1<T, E?>.In(values: Iterable<E>) where T : DocType, T : DocTypeAbility.Query, E : Enum<E>, E : FrappeEnum<E> =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.In, values.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("notInIterableEnum")
infix fun <T, E> KProperty1<T, E?>.notIn(values: Iterable<E>) where T : DocType, T : DocTypeAbility.Query, E : Enum<E>, E : FrappeEnum<E> =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotIn, values.toFrappeFilterValue()))
