package default_code.model.filter

import default_code.model.FrappeFilter
import default_code.util.LocalDateStringSerializer
import kotlinx.datetime.LocalDate
import kotlin.reflect.KProperty1
import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilterSet
import default_code.util.frappeName


@JvmInline
value class FrappeFilterDate(val data: LocalDate) : FrappeFilterValue {
    override fun serialize() = "\"${LocalDateStringSerializer.serialize(data)}\""
}

@JvmInline
value class FrappeFilterDatePair(val data: Pair<LocalDate, LocalDate>) : FrappeFilterValue {
    override fun serialize() =
        "[\"${LocalDateStringSerializer.serialize(data.first)}\",\"${LocalDateStringSerializer.serialize(data.second)}\"]"
}

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDate?>.eq(value: LocalDate) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, FrappeFilterDate(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDate?>.notEq(value: LocalDate) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotEq, FrappeFilterDate(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDate?>.after(value: LocalDate) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.After, FrappeFilterDate(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDate?>.before(value: LocalDate) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Before, FrappeFilterDate(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("betweenPairLocalDate")
infix fun <T> KProperty1<T, LocalDate?>.between(value: Pair<LocalDate, LocalDate>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Between, FrappeFilterDatePair(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("notBetweenPairLocalDate")
infix fun <T> KProperty1<T, LocalDate?>.notBetween(value: Pair<LocalDate, LocalDate>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotBetween, FrappeFilterDatePair(value)))
