package com.alexjlockwood.twentyfortyeight.domain

import com.alexjlockwood.twentyfortyeight.ui.EMPTY_GRID
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val grid: List<List<Tile?>>,
    val currentScore: Int,
    val bestScore: Int
) {
    companion object {
        val EMPTY_USER_DATA = UserData(grid = EMPTY_GRID, currentScore = 0, bestScore = 0)
    }
}
