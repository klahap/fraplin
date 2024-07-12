package default_code.model.filter

import default_code.model.FrappeFilter
import kotlin.reflect.KProperty1
import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilterSet
import default_code.util.LocalDateStringSerializer
import default_code.util.LocalDateTimeStringSerializer
import default_code.util.frappeName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime


@JvmInline
value class FrappeFilterDateTime(val data: LocalDateTime) : FrappeFilterValue {
    override fun serialize() = "\"${LocalDateTimeStringSerializer.serialize(data)}\""
}

@JvmInline
value class FrappeFilterDateTimePair(val data: Pair<LocalDateTime, LocalDateTime>) : FrappeFilterValue {
    override fun serialize() =
        "[\"${LocalDateTimeStringSerializer.serialize(data.first)}\",\"${LocalDateTimeStringSerializer.serialize(data.second)}\"]"
}

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDateTime?>.after(value: LocalDateTime) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.After, FrappeFilterDateTime(value)))

context(FrappeFilterSet.Builder<T>)
infix fun <T> KProperty1<T, LocalDateTime?>.before(value: LocalDateTime) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Before, FrappeFilterDateTime(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("betweenPairLocalDateTime")
infix fun <T> KProperty1<T, LocalDateTime?>.between(value: Pair<LocalDateTime, LocalDateTime>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Between, FrappeFilterDateTimePair(value)))

context(FrappeFilterSet.Builder<T>)
@JvmName("notBetweenPairLocalDateTime")
infix fun <T> KProperty1<T, LocalDateTime?>.notBetween(value: Pair<LocalDateTime, LocalDateTime>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotBetween, FrappeFilterDateTimePair(value)))
