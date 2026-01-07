package com.alexjlockwood.twentyfortyeight.repository

import com.alexjlockwood.twentyfortyeight.domain.MutableLimitedList
import com.alexjlockwood.twentyfortyeight.domain.UserData

/**
 * Statement of the 2048 game.
 */
class GameState(
    val gameRepository: GameRepository,
    val mutableStack: MutableList<UserData> = MutableLimitedList(mutableListOf(), 100),
)
