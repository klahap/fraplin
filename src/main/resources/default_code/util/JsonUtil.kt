package default_code.util

import default_code.model.FrappeAttachField
import default_code.model.FrappeDocStatus
import default_code.model.FrappeInlineStringField
import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


open class SafeSerializer<T>(
    private val serializer: KSerializer<T>
) : KSerializer<T?> {
    override val descriptor = serializer.descriptor

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: T?) =
        value?.let { encoder.encodeSerializableValue(serializer, it) } ?: encoder.encodeNull()

    override fun deserialize(decoder: Decoder): T? =
        runCatching { decoder.decodeSerializableValue(serializer) }.getOrNull()
}

object BooleanAsIntSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanAsInt", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeInt(if (value) 1 else 0)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeInt() != 0
}

object FrappeDocStatusSerializer : KSerializer<FrappeDocStatus> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FrappeDocStatus", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: FrappeDocStatus) = encoder.encodeInt(value.value)
    override fun deserialize(decoder: Decoder): FrappeDocStatus = decoder.decodeInt().let { status ->
        FrappeDocStatus.values().firstOrNull { it.value == status }
            ?: throw Exception("invalid DocStatus '$status'")
    }
}

abstract class FrappeInlineStringFieldSerializer<T : FrappeInlineStringField>(
    private val generator: (String) -> T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FrappeInlineStringField", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder) = generator(decoder.decodeString())

    object AttachField : FrappeInlineStringFieldSerializer<FrappeAttachField>({ FrappeAttachField(it) })
}

abstract class FrappeInlineStringFieldNullableSerializer<T : FrappeInlineStringField>(
    private val generator: (String) -> T,
) : KSerializer<T?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FrappeInlineStringFieldNullable", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: T?) =
        value?.value?.let(encoder::encodeString) ?: encoder.encodeNull()

    override fun deserialize(decoder: Decoder) =
        runCatching { decoder.decodeString() }.getOrNull()?.let { generator(it) }

    object AttachField : FrappeInlineStringFieldNullableSerializer<FrappeAttachField>({ FrappeAttachField(it) })
}

interface StringSerializer<T : Any> {
    val serialName: String
    fun serialize(value: T): String
    fun deserialize(value: String): T
}


interface BaseDateTimeSerializer<T : Any> : KSerializer<T>, StringSerializer<T> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) =
        serialize(value).let(encoder::encodeString)

    override fun deserialize(decoder: Decoder): T =
        deserialize(decoder.decodeString())
}

interface BaseDateTimeNullableSerializer<T : Any> : KSerializer<T?>, StringSerializer<T> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(serialName + "Nullable", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: T?) =
        value?.let { serialize(it) }?.let(encoder::encodeString) ?: encoder.encodeNull()

    override fun deserialize(decoder: Decoder): T? =
        runCatching { decoder.decodeString() }.getOrNull()?.let { deserialize(it) }
}


object LocalDateStringSerializer : StringSerializer<LocalDate> {
    override val serialName = "LocalDate"
    override fun serialize(value: LocalDate) = value.format(format)
    override fun deserialize(value: String) = LocalDate.parse(value, format)

    private val format = LocalDate.Format { date(LocalDate.Formats.ISO) }
}

object LocalDateTimeStringSerializer : StringSerializer<LocalDateTime> {
    override val serialName = "LocalDateTime"
    override fun serialize(value: LocalDateTime) = value.toInstant(timeZone).format(format)
    override fun deserialize(value: String) = Instant.parse(value, format).toLocalDateTime(timeZone)

    private val format = DateTimeComponents.Format {
        date(LocalDate.Formats.ISO)
        char(' ')
        hour(); char(':'); minute(); char(':'); second()
        alternativeParsing({}) {
            char('.'); secondFraction(fixedLength = 6)
        }
    }
    private val timeZone = TimeZone.currentSystemDefault()
}

object LocalTimeStringSerializer : StringSerializer<LocalTime> {
    override val serialName = "LocalTime"
    override fun serialize(value: LocalTime) = value.format(format)
    override fun deserialize(value: String) = LocalTime.parse(value, format)

    private val format = LocalTime.Format {
        hour(); char(':'); minute(); char(':'); second()
        alternativeParsing({}) {
            char('.'); secondFraction(fixedLength = 6)
        }
    }
}


object LocalDateSerializer
    : BaseDateTimeSerializer<LocalDate>, StringSerializer<LocalDate> by LocalDateStringSerializer

object LocalDateTimeSerializer
    : BaseDateTimeSerializer<LocalDateTime>, StringSerializer<LocalDateTime> by LocalDateTimeStringSerializer

object LocalTimeSerializer
    : BaseDateTimeSerializer<LocalTime>, StringSerializer<LocalTime> by LocalTimeStringSerializer


object LocalDateNullableSerializer
    : BaseDateTimeNullableSerializer<LocalDate>, StringSerializer<LocalDate> by LocalDateStringSerializer

object LocalDateTimeNullableSerializer
    : BaseDateTimeNullableSerializer<LocalDateTime>, StringSerializer<LocalDateTime> by LocalDateTimeStringSerializer

object LocalTimeNullableSerializer
    : BaseDateTimeNullableSerializer<LocalTime>, StringSerializer<LocalTime> by LocalTimeStringSerializer
