package com.alexjlockwood.twentyfortyeight.domain

import androidx.compose.runtime.Immutable

/**
 * Container class that describes a location in a 2D grid.
 */
@Immutable
data class Cell(val row: Int, val col: Int)
