package com.alexjlockwood.twentyfortyeight.repository

import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.ui.GRID_SIZE
import io.github.xxfast.kstore.KStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

const val USER_DATA_FILE_NAME = "user_data_key_value.json"
private const val KEY_GRID = "grid"
private const val KEY_CURRENT_SCORE = "current_score"
private const val KEY_BEST_SCORE = "best_score"

val PolymorphismJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    useArrayPolymorphism = true

    serializersModule = SerializersModule {
        polymorphic(Any::class) {
            subclass(Int::class)
            subclass(IntArray::class)
        }
    }
}

/**
 * Repository class that persists the current 2048 game.
 */
interface GameRepository {

    suspend fun fetch(): UserData

    suspend fun update(grid: List<List<Tile?>>, currentScore: Int, bestScore: Int)
}

class DefaultGameRepository(
    private val store: KStore<Map<String, Any>>,
) : GameRepository {

    override suspend fun fetch(): UserData {
        val map = store.get() ?: return UserData.EMPTY_USER_DATA
        return UserData(
            grid = (map[KEY_GRID] as? IntArray)?.toTiles(GRID_SIZE) ?: UserData.EMPTY_USER_DATA.grid,
            currentScore = map[KEY_CURRENT_SCORE] as? Int ?: UserData.EMPTY_USER_DATA.currentScore,
            bestScore = map[KEY_BEST_SCORE] as? Int ?: UserData.EMPTY_USER_DATA.bestScore,
        )
    }

    override suspend fun update(grid: List<List<Tile?>>, currentScore: Int, bestScore: Int) {
        val map: Map<String, Any> = mapOf(
            KEY_GRID to grid.toIntArray(),
            KEY_CURRENT_SCORE to currentScore,
            KEY_BEST_SCORE to bestScore,
        )
        store.set(map)
    }
}

private fun IntArray.toTiles(size: Int): List<List<Tile?>> {
    return map { if (it == 0) null else Tile(it) }.chunked(size)
}

private fun List<List<Tile?>>.toIntArray(): IntArray {
    return flatMap { list -> list.map { it?.num ?: 0 } }.toIntArray()
}
