package default_code.model.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName
import kotlin.reflect.KProperty1


@JvmInline
value class FrappeFilterDouble(val data: Double) : FrappeFilterValue {
    override fun serialize() = data.toString()
}
@JvmInline
value class FrappeFilterDoublePair(val data: Pair<Double, Double>) : FrappeFilterValue {
    override fun serialize() = "[${data.first},${data.second}]"
}


context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Double?>.gt(value: Double) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Gt, FrappeFilterDouble(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Double?>.gte(value: Double) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Gte, FrappeFilterDouble(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Double?>.lt(value: Double) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Lt, FrappeFilterDouble(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Double?>.lte(value: Double) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Lte, FrappeFilterDouble(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("betweenPairDouble")
infix fun <T> KProperty1<T, Double?>.between(value: Pair<Double, Double>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Between, FrappeFilterDoublePair(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("notBetweenPairDouble")
infix fun <T> KProperty1<T, Double?>.notBetween(value: Pair<Double, Double>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotBetween, FrappeFilterDoublePair(value)))
