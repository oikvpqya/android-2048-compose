package com.alexjlockwood.twentyfortyeight

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.BackHandler
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.viewmodel.GameViewModel

@Composable
fun App(
    dataStore: DataStore<Preferences>,
    isPortrait: Boolean = true
) {
    val gameViewModel = viewModel { GameViewModel(GameRepository(dataStore)) }
    val isDarkTheme = isSystemInDarkTheme()
    BackHandler(gameViewModel.canUndo) { gameViewModel.undo() }
    AppTheme(isDarkTheme) {
        Surface {
            GameUi(
                gridTileMovements = gameViewModel.gridTileMovements,
                currentScore = gameViewModel.currentScore,
                bestScore = gameViewModel.bestScore,
                moveCount = gameViewModel.moveCount,
                canUndo = gameViewModel.canUndo,
                isGameOver = gameViewModel.isGameOver,
                isDarkTheme = isDarkTheme,
                isPortrait = isPortrait,
                onNewGameRequested = { gameViewModel.startNewGame() },
                onUndoGameRequested = { gameViewModel.undo() },
                onSwipeListener = { direction -> gameViewModel.move(direction) },
            )
        }
    }
}
