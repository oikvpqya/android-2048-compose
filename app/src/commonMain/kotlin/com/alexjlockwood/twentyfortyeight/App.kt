package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.BackHandler
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.viewmodel.GameViewModel

@Composable
fun App(repository: GameRepository) {
    val gameViewModel = viewModel { GameViewModel(repository) }
    BackHandler(gameViewModel.canUndo) { gameViewModel.undo() }
    AppTheme {
        Surface {
            GameUi(
                gridTileMovements = gameViewModel.gridTileMovements,
                currentScore = gameViewModel.currentScore,
                bestScore = gameViewModel.bestScore,
                canUndo = gameViewModel.canUndo,
                isGameOver = gameViewModel.isGameOver,
                onNewGameRequested = { gameViewModel.startNewGame() },
                onUndoGameRequested = { gameViewModel.undo() },
                onSwipeListener = { direction -> gameViewModel.move(direction) },
            )
        }
    }
}
