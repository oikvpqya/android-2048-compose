package com.alexjlockwood.twentyfortyeight.repository

import com.alexjlockwood.twentyfortyeight.domain.UserData

const val USER_DATA_FILE_NAME = "user_data.json"

/**
 * Repository class that persists the current 2048 game.
 */
interface GameRepository {

    suspend fun fetch(): UserData

    suspend fun update(data: UserData)
}
