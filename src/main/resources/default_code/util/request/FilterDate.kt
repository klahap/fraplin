package default_code.filter

import default_code.model.FrappeFilter
import default_code.util.LocalDateStringSerializer
import kotlinx.datetime.LocalDate
import kotlin.reflect.KProperty1
import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilterSet
import default_code.util.frappeName


fun LocalDate.toFrappeFilterValue() = FrappeFilter.Value(
    "\"${LocalDateStringSerializer.serialize(this)}\""
)

@JvmName("toFilterValuePairLocalDate")
fun Pair<LocalDate, LocalDate>.toFrappeFilterValue() = FrappeFilter.Value(
    "[\"${LocalDateStringSerializer.serialize(first)}\",\"${LocalDateStringSerializer.serialize(second)}\"]"
)

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDate?>.eq(value: LocalDate) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDate?>.notEq(value: LocalDate) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotEq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDate?>.after(value: LocalDate) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.After, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDate?>.before(value: LocalDate) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Before, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("betweenPairLocalDate")
infix fun <T> KProperty1<T, LocalDate?>.between(value: Pair<LocalDate, LocalDate>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Between, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("notBetweenPairLocalDate")
infix fun <T> KProperty1<T, LocalDate?>.notBetween(value: Pair<LocalDate, LocalDate>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotBetween, value.toFrappeFilterValue()))
