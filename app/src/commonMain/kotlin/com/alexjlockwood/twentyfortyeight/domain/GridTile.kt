package com.alexjlockwood.twentyfortyeight.domain

import androidx.compose.runtime.Immutable

/**
 * Container class describing a tile at a certain location in the grid.
 */
@Immutable
data class GridTile(val cell: Cell, val tile: Tile)
