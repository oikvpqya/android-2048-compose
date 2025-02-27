package com.alexjlockwood.twentyfortyeight.domain

import kotlinx.serialization.Serializable

/**
 * Container class describing a tile at a certain location in the grid.
 */
@Serializable
data class GridTile(val cell: Cell, val tile: Tile)
