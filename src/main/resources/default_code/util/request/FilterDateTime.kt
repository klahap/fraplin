package default_code.filter

import default_code.model.FrappeFilter
import kotlin.reflect.KProperty1
import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilterSet
import default_code.util.LocalDateTimeStringSerializer
import default_code.util.frappeName
import kotlinx.datetime.LocalDateTime


fun LocalDateTime.toFrappeFilterValue() = FrappeFilter.Value(
    "\"${LocalDateTimeStringSerializer.serialize(this)}\""
)

@JvmName("toFilterValuePairLocalDateTime")
fun Pair<LocalDateTime, LocalDateTime>.toFrappeFilterValue() = FrappeFilter.Value(
    "[\"${LocalDateTimeStringSerializer.serialize(first)}\",\"${LocalDateTimeStringSerializer.serialize(second)}\"]"
)

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDateTime?>.after(value: LocalDateTime) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.After, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDateTime?>.before(value: LocalDateTime) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Before, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("betweenPairLocalDateTime")
infix fun <T> KProperty1<T, LocalDateTime?>.between(value: Pair<LocalDateTime, LocalDateTime>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Between, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("notBetweenPairLocalDateTime")
infix fun <T> KProperty1<T, LocalDateTime?>.notBetween(value: Pair<LocalDateTime, LocalDateTime>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotBetween, value.toFrappeFilterValue()))
