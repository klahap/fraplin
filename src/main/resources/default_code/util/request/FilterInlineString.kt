package default_code.filter

import default_code.DocType
import default_code.DocTypeAbility
import default_code.model.FrappeFilter
import default_code.model.FrappeFilterSet
import default_code.model.FrappeInlineStringField
import default_code.util.frappeName
import kotlin.reflect.KProperty1


fun FrappeInlineStringField.toFrappeFilterValue() = FrappeFilter.Value("\"${this.value}\"")

@JvmName("toFilterValueInlineString")
fun <S : FrappeInlineStringField> Iterable<S>.toFrappeFilterValue() =
    FrappeFilter.Value(toSet().joinToString(separator = ",", prefix = "[", postfix = "]") { "\"${it.value}\"" })


context(FrappeFilterSet.Builder<T>)
infix fun <T, S : FrappeInlineStringField> KProperty1<T, S?>.eq(value: S) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Eq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T, S : FrappeInlineStringField> KProperty1<T, S?>.notEq(value: S) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotEq, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("inIterableInlineString")
infix fun <T, S : FrappeInlineStringField> KProperty1<T, S?>.In(values: Iterable<S>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.In, values.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
@JvmName("notInIterableInlineString")
infix fun <T, S : FrappeInlineStringField> KProperty1<T, S?>.notIn(values: Iterable<S>) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotIn, values.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T, S : FrappeInlineStringField> KProperty1<T, S?>.like(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.Like, value.toFrappeFilterValue()))

context(FrappeFilterSet.Builder<T>)
infix fun <T, S : FrappeInlineStringField> KProperty1<T, S?>.notLike(value: String) where T : DocType, T : DocTypeAbility.Query =
    add(FrappeFilter(frappeName, FrappeFilter.Operator.NotLike, value.toFrappeFilterValue()))
