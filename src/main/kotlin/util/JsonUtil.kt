package io.github.klahap.fraplin.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

inline fun <reified T : Any> getAllSerialNames(): Set<String> {
    val validParams = T::class.primaryConstructor!!.parameters.mapNotNull { it.name }.toSet()
    return T::class.memberProperties
        .filter { validParams.contains(it.name) }
        .map { it.findAnnotation<SerialName>()!!.value }.toSet()
}

open class SafeSerializer<T>(
    private val serializer: KSerializer<T>
) : KSerializer<T?> {
    override val descriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: T?) =
        encoder.encodeSerializableValue(serializer, value!!)

    override fun deserialize(decoder: Decoder): T? = try {
        decoder.decodeSerializableValue(serializer)
    } catch (_: Exception) {
        null
    }
}

object BooleanAsIntSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanAsInt", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeInt(if (value) 1 else 0)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeInt() != 0
}
