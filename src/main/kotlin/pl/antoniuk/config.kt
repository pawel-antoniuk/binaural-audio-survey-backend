package pl.antoniuk

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule

object FlexibleFloatSerializer : KSerializer<Float> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleFloat", PrimitiveKind.FLOAT)

    override fun deserialize(decoder: Decoder): Float {
        val input = (decoder as? JsonDecoder)?.decodeJsonElement()
        return when (input) {
            is JsonPrimitive -> input.content.toFloatOrNull()
                ?: throw SerializationException("Invalid float format: ${input.content}")
            else -> throw SerializationException("Unexpected JSON element for Float")
        }
    }

    override fun serialize(encoder: Encoder, value: Float) {
        encoder.encodeFloat(value)
    }
}

// Create the global Json instance
val customJson = Json {
    serializersModule = SerializersModule {
        contextual(Float::class, FlexibleFloatSerializer) // Apply the serializer globally
    }
    ignoreUnknownKeys = true // Optional: Ignore extra fields
}