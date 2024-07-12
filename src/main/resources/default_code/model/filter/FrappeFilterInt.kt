package default_code.model.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.util.frappeName
import kotlin.reflect.KProperty1


@JvmInline
value class FrappeFilterInt(val data: Int) : FrappeFilterValue {
    override fun serialize() = data.toString()
}


@JvmInline
value class FrappeFilterIntPair(val data: Pair<Int, Int>) : FrappeFilterValue {
    override fun serialize() = "[${data.first},${data.second}]"
}

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.eq(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, FrappeFilterInt(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.notEq(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotEq, FrappeFilterInt(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.gt(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Gt, FrappeFilterInt(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.gte(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Gte, FrappeFilterInt(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.lt(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Lt, FrappeFilterInt(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, Int?>.lte(value: Int) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Lte, FrappeFilterInt(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("betweenPairInt")
infix fun <T> KProperty1<T, Int?>.between(value: Pair<Int, Int>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Between, FrappeFilterIntPair(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("notBetweenPairInt")
infix fun <T> KProperty1<T, Int?>.notBetween(value: Pair<Int, Int>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotBetween, FrappeFilterIntPair(value)))
