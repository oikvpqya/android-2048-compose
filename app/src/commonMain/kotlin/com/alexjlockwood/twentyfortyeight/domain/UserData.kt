package com.alexjlockwood.twentyfortyeight.domain

data class UserData (
    val grid: List<List<Tile?>>?,
    val currentScore: Int,
    val bestScore: Int
)
