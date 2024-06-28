package com.alexjlockwood.twentyfortyeight.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class UserData (
    val grid: List<List<Tile?>>?,
    val currentScore: Int,
    val bestScore: Int
) {
    companion object {
        val EMPTY_USER_DATA = UserData(grid = null, currentScore = 0, bestScore = 0)
    }
}
