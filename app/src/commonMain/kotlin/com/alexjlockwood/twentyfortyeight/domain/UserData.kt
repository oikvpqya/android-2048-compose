package com.alexjlockwood.twentyfortyeight.domain

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val movements: List<GridTileMovement> = emptyList(),
    val currentScore: Int = 0,
    val bestScore: Int = 0,
)
