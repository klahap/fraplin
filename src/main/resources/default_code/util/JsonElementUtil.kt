package default_code.util

import default_code.FrappeEnum
import default_code.DocType
import default_code.model.FrappeAttachField
import default_code.model.FrappeDocStatus
import default_code.model.FrappeInlineStringField
import kotlinx.serialization.json.*
import kotlin.reflect.KClass


interface JsonElementType<T : Any> {
    fun toValue(data: JsonElement): T?
    fun toJson(data: T?): JsonElement

    data object Boolean : JsonElementType<kotlin.Boolean> {
        override fun toValue(data: JsonElement) = data.jsonPrimitive.booleanOrNull
        override fun toJson(data: kotlin.Boolean?) = JsonPrimitive(data)
    }

    data object Int : JsonElementType<kotlin.Int> {
        override fun toValue(data: JsonElement) = data.jsonPrimitive.intOrNull
        override fun toJson(data: kotlin.Int?) = JsonPrimitive(data)
    }

    data object Long : JsonElementType<kotlin.Long> {
        override fun toValue(data: JsonElement) = data.jsonPrimitive.longOrNull
        override fun toJson(data: kotlin.Long?) = JsonPrimitive(data)
    }

    data object Float : JsonElementType<kotlin.Float> {
        override fun toValue(data: JsonElement) = data.jsonPrimitive.floatOrNull
        override fun toJson(data: kotlin.Float?) = JsonPrimitive(data)
    }

    data object Double : JsonElementType<kotlin.Double> {
        override fun toValue(data: JsonElement) = data.jsonPrimitive.doubleOrNull
        override fun toJson(data: kotlin.Double?) = JsonPrimitive(data)
    }

    data object String : JsonElementType<kotlin.String> {
        override fun toValue(data: JsonElement) = data.jsonPrimitive.contentOrNull
        override fun toJson(data: kotlin.String?) = JsonPrimitive(data)
    }

    data object DocStatus : JsonElementType<FrappeDocStatus> {
        override fun toValue(data: JsonElement) = data.jsonPrimitive.intOrNull
            ?.let { status -> FrappeDocStatus.values().firstOrNull { it.value == status } }

        override fun toJson(data: FrappeDocStatus?) = JsonPrimitive(data?.value)
    }

    data object LocalDate : JsonElementType<kotlinx.datetime.LocalDate> {
        override fun toValue(data: JsonElement) =
            data.jsonPrimitive.contentOrNull?.let(LocalDateSerializer::deserialize)

        override fun toJson(data: kotlinx.datetime.LocalDate?) =
            JsonPrimitive(data?.let(LocalDateSerializer::serialize))
    }

    abstract class InlineString<T : FrappeInlineStringField>(
        private val generator: (kotlin.String) -> T,
    ) : JsonElementType<T> {
        override fun toValue(data: JsonElement): T? =
            data.jsonPrimitive.contentOrNull?.let(generator)

        override fun toJson(data: T?) = JsonPrimitive(data?.value)

        object Attach : InlineString<FrappeAttachField>({ FrappeAttachField(it) })
    }

    data object LocalDateTime : JsonElementType<kotlinx.datetime.LocalDateTime> {
        override fun toValue(data: JsonElement) =
            data.jsonPrimitive.contentOrNull?.let(LocalDateTimeSerializer::deserialize)

        override fun toJson(data: kotlinx.datetime.LocalDateTime?) =
            JsonPrimitive(data?.let(LocalDateTimeSerializer::serialize))
    }

    data object LocalTime : JsonElementType<kotlinx.datetime.LocalTime> {
        override fun toValue(data: JsonElement) =
            data.jsonPrimitive.contentOrNull?.let(LocalTimeSerializer::deserialize)

        override fun toJson(data: kotlinx.datetime.LocalTime?) =
            JsonPrimitive(data?.let(LocalTimeSerializer::serialize))
    }

    class Enum<E>(private val clazz: KClass<E>) : JsonElementType<E> where E : kotlin.Enum<E>, E : FrappeEnum<E> {
        override fun toValue(data: JsonElement) = FrappeEnum.originValueOf(clazz, data.jsonPrimitive.content)
        override fun toJson(data: E?) = JsonPrimitive(data?.origin)
    }

    class MutableList<T> :
        JsonElementType<kotlin.collections.MutableList<T>> where T : DocType.Child {
        override fun toValue(data: JsonElement) = when (data) {
            is JsonObject -> json.decodeFromJsonElement<kotlin.collections.MutableList<T>>(data)
            else -> null
        }

        override fun toJson(data: kotlin.collections.MutableList<T>?) =
            data?.let { json.encodeToJsonElement(it) } ?: JsonNull

        companion object {
            private val json = Json { ignoreUnknownKeys = true }
        }
    }

    class List<T> :
        JsonElementType<kotlin.collections.List<T>> where T : DocType.Child {
        override fun toValue(data: JsonElement) = when (data) {
            is JsonObject -> json.decodeFromJsonElement<kotlin.collections.MutableList<T>>(data)
            else -> null
        }

        override fun toJson(data: kotlin.collections.List<T>?) =
            data?.let { json.encodeToJsonElement(it) } ?: JsonNull

        companion object {
            private val json = Json { ignoreUnknownKeys = true }
        }
    }
}


interface JsonElementField<T> {
    val name: kotlin.String
    fun toEntry(value: T): Pair<kotlin.String, JsonElement>

    interface String : JsonElementField<kotlin.String> {
        override fun toEntry(value: kotlin.String) = name to JsonElementType.String.toJson(value)
    }

    interface StringNullable : JsonElementField<kotlin.String?> {
        override fun toEntry(value: kotlin.String?) = name to JsonElementType.String.toJson(value)
    }

    interface Boolean : JsonElementField<kotlin.Boolean> {
        override fun toEntry(value: kotlin.Boolean) = name to JsonElementType.Boolean.toJson(value)
    }

    interface BooleanNullable : JsonElementField<kotlin.Boolean?> {
        override fun toEntry(value: kotlin.Boolean?) = name to JsonElementType.Boolean.toJson(value)
    }

    interface Long : JsonElementField<kotlin.Long> {
        override fun toEntry(value: kotlin.Long) = name to JsonElementType.Long.toJson(value)
    }

    interface LongNullable : JsonElementField<kotlin.Long?> {
        override fun toEntry(value: kotlin.Long?) = name to JsonElementType.Long.toJson(value)
    }

    interface Double : JsonElementField<kotlin.Double> {
        override fun toEntry(value: kotlin.Double) = name to JsonElementType.Double.toJson(value)
    }

    interface DoubleNullable : JsonElementField<kotlin.Double?> {
        override fun toEntry(value: kotlin.Double?) = name to JsonElementType.Double.toJson(value)
    }

    interface Any : JsonElementField<JsonElement> {
        override fun toEntry(value: JsonElement) = name to value
    }

    interface AnyNullable : JsonElementField<JsonElement?> {
        override fun toEntry(value: JsonElement?) = name to (value ?: JsonNull)
    }
}