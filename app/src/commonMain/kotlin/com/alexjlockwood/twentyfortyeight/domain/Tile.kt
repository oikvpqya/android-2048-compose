package com.alexjlockwood.twentyfortyeight.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Container class that wraps a number and a unique ID for use in the grid.
 */
@Serializable(TileSerializer::class)
data class Tile(
    val num: Int,
    val id: Int = tileIdCounter++,
) {
    companion object {
        // We assign each tile a unique ID and use it to efficiently
        // animate tile objects within the compose UI.
        private var tileIdCounter = 0
    }

    operator fun times(operand: Int): Tile = Tile(num * operand)
}

object TileSerializer : KSerializer<Tile> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.alexjlockwood.twentyfortyeight.domain.TileSerializer",
        PrimitiveKind.INT,
    )

    override fun serialize(encoder: Encoder, value: Tile) {
        encoder.encodeInt(value.num)
    }

    override fun deserialize(decoder: Decoder): Tile {
        return Tile(decoder.decodeInt())
    }
}
