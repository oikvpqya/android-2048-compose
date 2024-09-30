package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.BackHandler
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.ui.GameUiState
import com.alexjlockwood.twentyfortyeight.ui.gamePresenter
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
fun App(repository: GameRepository) {
    val eventFlow = remember { MutableSharedFlow<GameUiEvent>(extraBufferCapacity = 20) }
    val uiState = gamePresenter(eventFlow, repository).let { state ->
        when (state) {
            GameUiState.Loading -> return
            is GameUiState.Success -> state
        }
    }

    BackHandler(uiState.canUndo) { eventFlow.tryEmit(GameUiEvent.Undo) }
    AppTheme {
        Surface {
            GameUi(
                gridTileMovements = uiState.gridTileMovements,
                currentScore = uiState.currentScore,
                bestScore = uiState.bestScore,
                canUndo = uiState.canUndo,
                isGameOver = uiState.isGameOver,
                onNewGameRequested = { eventFlow.tryEmit(GameUiEvent.StartNewGame) },
                onUndoGameRequested = { eventFlow.tryEmit(GameUiEvent.Undo) },
                onSwipeListener = { eventFlow.tryEmit(GameUiEvent.Move(it)) },
            )
        }
    }
}
