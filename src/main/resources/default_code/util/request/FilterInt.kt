package default_code.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName
import kotlin.reflect.KProperty1

fun Int.toFrappeFilterValue() = FrappeFilter.Value("$this")

@JvmName("toFilterValuePairInt")
fun Pair<Int, Int>.toFrappeFilterValue() = FrappeFilter.Value("[$first,$second]")

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.eq(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.notEq(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotEq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.gt(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Gt, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.gte(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Gte, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.lt(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Lt, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.lte(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Lte, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("betweenPairInt")
infix fun <T> KProperty1<T, Int?>.between(value: Pair<Int, Int>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Between, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("notBetweenPairInt")
infix fun <T> KProperty1<T, Int?>.notBetween(value: Pair<Int, Int>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotBetween, value.toFrappeFilterValue()))
