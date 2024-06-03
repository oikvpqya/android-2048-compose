package com.alexjlockwood.twentyfortyeight.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val USER_DATA_FILE_NAME = "user_data.preferences_pb"
private val KEY_GRID = stringPreferencesKey("grid")
private val KEY_CURRENT_SCORE = intPreferencesKey("current_score")
private val KEY_BEST_SCORE = intPreferencesKey("best_score")

/**
 * Repository class that persists the current 2048 game to DataStore.
 */
class GameRepository(private val dataStore: DataStore<Preferences>) {

    val userDataFlow: Flow<UserData> = dataStore.data
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
        dataStore.edit { preferences ->
            preferences[KEY_GRID] = Json.encodeToString(grid.map { tiles -> tiles.map { it?.num } })
            preferences[KEY_CURRENT_SCORE] = currentScore
            preferences[KEY_BEST_SCORE] = bestScore
        }
    }
}
