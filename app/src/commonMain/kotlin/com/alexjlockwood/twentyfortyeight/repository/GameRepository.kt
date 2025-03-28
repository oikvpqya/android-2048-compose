package com.alexjlockwood.twentyfortyeight.repository

import com.alexjlockwood.twentyfortyeight.domain.UserData
import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

const val USER_DATA_FILE_NAME = "user_data.json"

/**
 * Repository class that persists the current 2048 game.
 */
interface GameRepository {

    suspend fun fetch(): UserData

    suspend fun update(data: UserData)
}

class DefaultGameRepository(
    private val store: KStore<UserData>,
    private val dispatcher: CoroutineContext = EmptyCoroutineContext,
) : GameRepository {

    override suspend fun fetch(): UserData {
        return withContext(dispatcher) { store.get() ?: UserData.EMPTY_USER_DATA }
    }

    override suspend fun update(data: UserData) {
        withContext(dispatcher) { store.set(data) }
    }
}
