package com.alexjlockwood.twentyfortyeight.repository

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.alexjlockwood.twentyfortyeight.domain.UserData

const val USER_DATA_FILE_NAME = "user_data.json"

/**
 * Repository class that persists the current 2048 game.
 */
interface GameRepository {

    suspend fun fetch(): UserData

    suspend fun update(data: UserData)
}

val LocalGameRepository: ProvidableCompositionLocal<GameRepository> = staticCompositionLocalOf {
    error("CompositionLocal GameRepository not provided")
}
