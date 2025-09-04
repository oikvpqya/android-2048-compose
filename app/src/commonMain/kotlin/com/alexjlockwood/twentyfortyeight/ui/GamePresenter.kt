package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameState
import com.alexjlockwood.twentyfortyeight.repository.checkIsGameOver
import com.alexjlockwood.twentyfortyeight.runtime.EventBusImpl
import com.alexjlockwood.twentyfortyeight.runtime.Presenter
import com.alexjlockwood.twentyfortyeight.runtime.PresenterImpl
import kotlin.coroutines.CoroutineContext

sealed interface GameUiEvent {

    data object Load : GameUiEvent
    data class Move(val direction: Direction) : GameUiEvent
    data object StartNewGame : GameUiEvent
    data object Undo : GameUiEvent
}

sealed interface GameUiState {

    data object Loading : GameUiState
    data object Nothing : GameUiState
    data class Success(
        val gridTileMovements: List<GridTileMovement>,
        val currentScore: Int,
        val bestScore: Int,
        val isGameOver: Boolean,
        val canUndo: Boolean,
    ) : GameUiState {

        constructor(
            data: UserData,
            canUndo: Boolean,
        ) : this(
            gridTileMovements = data.movements,
            currentScore = data.currentScore,
            bestScore = data.bestScore,
            isGameOver = data.checkIsGameOver(),
            canUndo = canUndo,
        )
    }
}

@Composable
fun rememberGamePresenter(
    gameState: GameState,
): Presenter<GameUiEvent, GameUiState> {
    return remember(
        gameState,
    ) { GamePresenter(PresenterImpl(EventBusImpl(), GameUiState.Nothing), gameState) }
}

class GamePresenter(
    base: Presenter<GameUiEvent, GameUiState>,
    private val gameState: GameState,
) : Presenter<GameUiEvent, GameUiState> by base {

    private suspend fun load() {
        gameState.load(true)?.let { data ->
            produceUiState(GameUiState.Success(data, gameState.stack.isNotEmpty()))
            return
        }
        produceUiState(GameUiState.Loading)
        gameState.load(false)?.let { data ->
            produceUiState(GameUiState.Success(data, false))
            return
        }
        produceUiState(GameUiState.Success(gameState.startNewGame(), false))
    }

    override suspend fun handleEvent(event: GameUiEvent, coroutineContext: CoroutineContext) {
        when (event) {
            GameUiEvent.Load -> {
                load()
            }
            is GameUiEvent.Move -> {
                gameState.move(event.direction)?.let { data ->
                    produceUiState(GameUiState.Success(data, gameState.stack.isNotEmpty()))
                }
            }
            GameUiEvent.StartNewGame -> {
                produceUiState(GameUiState.Success(gameState.startNewGame(), false))
            }
            GameUiEvent.Undo -> {
                gameState.undo()?.let { data ->
                    produceUiState(GameUiState.Success(data, gameState.stack.isNotEmpty()))
                }
            }
        }
    }
}
