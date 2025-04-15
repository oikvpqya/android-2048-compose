package com.alexjlockwood.twentyfortyeight.domain

import kotlinx.serialization.Serializable

/**
 * Container class describing how a tile has moved within the grid.
 */
@Serializable
data class GridTileMovement(
    val tile: Tile,
    val from: Cell?,
    val to: Cell,
) {
    companion object {
        /**
         * Creates a [GridTileMovement] describing a tile that has been added to the grid.
         */
        fun add(tile: Tile, cell: Cell): GridTileMovement {
            return GridTileMovement(tile, null, cell)
        }

        /**
         * Creates a [GridTileMovement] describing a tile that has shifted to a different location in the grid.
         */
        fun shift(tile: Tile, from: Cell, to: Cell): GridTileMovement {
            return GridTileMovement(tile, from, to)
        }

        /**
         * Creates a [GridTileMovement] describing a tile that has not moved in the grid.
         */
        fun noop(tile: Tile, cell: Cell): GridTileMovement {
            return GridTileMovement(tile, cell, cell)
        }
    }
}
