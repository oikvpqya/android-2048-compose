package com.alexjlockwood.twentyfortyeight.domain

data class UserData(
    val movements: List<GridTileMovement> = emptyList(),
    val currentScore: Int = 0,
    val bestScore: Int = 0,
)
