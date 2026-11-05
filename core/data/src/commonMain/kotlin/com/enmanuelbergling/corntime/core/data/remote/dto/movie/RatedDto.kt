package com.enmanuelbergling.corntime.core.data.remote.dto.movie

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class RatedDto(
    val value: Float,
)

/**
 * TMDB returns `"rated": false` when the item is not rated and `"rated": { "value": x }` when it
 * is. This serializer coerces the `false` case to `null` so [RatedDto] can stay nullable.
 */
internal object RatedDTOSerializer : KSerializer<RatedDto?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RatedDto")

    override fun deserialize(decoder: Decoder): RatedDto? {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return if (element is JsonObject) {
            jsonDecoder.json.decodeFromJsonElement(RatedDto.serializer(), element)
        } else {
            null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: RatedDto?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(RatedDto.serializer(), value)
        }
    }
}
