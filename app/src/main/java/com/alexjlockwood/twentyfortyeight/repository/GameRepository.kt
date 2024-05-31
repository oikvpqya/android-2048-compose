package com.alexjlockwood.twentyfortyeight.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alexjlockwood.twentyfortyeight.domain.Tile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "preferences")
private val KEY_GRID = stringPreferencesKey("grid")
private val KEY_CURRENT_SCORE = intPreferencesKey("current_score")
private val KEY_BEST_SCORE = intPreferencesKey("best_score")

data class UserData (
    val grid: List<List<Tile?>>?,
    val currentScore: Int,
    val bestScore: Int
)

/**
 * Repository class that persists the current 2048 game to DataStore.
 */
class GameRepository(private val context: Context) {

    val userDataFlow: Flow<UserData> = context.dataStore.data
        .map { preferences ->
            val grid = preferences[KEY_GRID]
                ?.let {
                    Json.decodeFromString<List<List<Int?>>?>(it)
                }
                ?.map { tiles ->
                    tiles.map { if (it == null) null else Tile(it) }
                }
            val currentScore = preferences[KEY_CURRENT_SCORE] ?: 0
            val bestScore = preferences[KEY_BEST_SCORE] ?: 0
            UserData(grid, currentScore, bestScore)
        }

    suspend fun saveState(grid: List<List<Tile?>>, currentScore: Int, bestScore: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GRID] = Json.encodeToString(grid.map { tiles -> tiles.map { it?.num } })
            preferences[KEY_CURRENT_SCORE] = currentScore
            preferences[KEY_BEST_SCORE] = bestScore
        }
    }
}