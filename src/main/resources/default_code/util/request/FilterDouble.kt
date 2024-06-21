package default_code.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName
import kotlin.reflect.KProperty1

fun Double.toFrappeFilterValue() = FrappeFilter.Value("$this")

@JvmName("toFilterValuePairDouble")
fun Pair<Double, Double>.toFrappeFilterValue() = FrappeFilter.Value("[$first,$second]")


context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Double?>.gt(value: Double) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Gt, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Double?>.gte(value: Double) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Gte, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Double?>.lt(value: Double) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Lt, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Double?>.lte(value: Double) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Lte, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("betweenPairDouble")
infix fun <T> KProperty1<T, Double?>.between(value: Pair<Double, Double>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Between, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("notBetweenPairDouble")
infix fun <T> KProperty1<T, Double?>.notBetween(value: Pair<Double, Double>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotBetween, value.toFrappeFilterValue()))
