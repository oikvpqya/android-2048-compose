package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameState
import com.alexjlockwood.twentyfortyeight.repository.checkIsGameOver
import com.alexjlockwood.twentyfortyeight.runtime.EventBus
import com.alexjlockwood.twentyfortyeight.runtime.Presenter
import com.alexjlockwood.twentyfortyeight.runtime.buildEventBus
import com.alexjlockwood.twentyfortyeight.runtime.buildPresenter
import com.alexjlockwood.twentyfortyeight.runtime.rememberEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    eventBus: EventBus<GameUiEvent> = rememberEventBus(),
): Presenter<GameUiEvent, GameUiState> {
    val coroutineScope = rememberCoroutineScope()
    return remember(
        gameState,
    ) { GamePresenter(gameState, coroutineScope, eventBus) }
}

class GamePresenter(
    private val gameState: GameState,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    eventBus: EventBus<GameUiEvent> = buildEventBus(),
) : Presenter<GameUiEvent, GameUiState> by buildPresenter(GameUiState.Nothing, eventBus), RememberObserver {

    private var job: Job? = null

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

    override suspend fun handleEvent(event: GameUiEvent) {
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

    private fun startJob(): Job {
        return coroutineScope.launch {
            load()
        }
    }

    private fun stop() {
        job?.cancel()
        job = null
    }

    override fun onRemembered() {
        job = startJob()
    }

    override fun onForgotten() {
        stop()
    }

    override fun onAbandoned() {
        stop()
    }
}
