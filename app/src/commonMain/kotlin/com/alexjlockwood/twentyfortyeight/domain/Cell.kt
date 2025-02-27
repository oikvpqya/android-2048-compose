package com.alexjlockwood.twentyfortyeight.domain

import kotlinx.serialization.Serializable

/**
 * Container class that describes a location in a 2D grid.
 */
@Serializable
data class Cell(val row: Int, val col: Int)
