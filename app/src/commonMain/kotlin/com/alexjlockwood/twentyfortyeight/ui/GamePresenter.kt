package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GameStrategy
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.GameState
import com.alexjlockwood.twentyfortyeight.runtime.EventBus
import com.alexjlockwood.twentyfortyeight.runtime.Presenter
import com.alexjlockwood.twentyfortyeight.runtime.buildEventBus
import com.alexjlockwood.twentyfortyeight.runtime.buildPresenter
import com.alexjlockwood.twentyfortyeight.runtime.rememberEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val GRID_SIZE = GameStrategy.DEFAULT.gridSize

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
        val gridSize: Int,
        val gridTileMovements: List<GridTileMovement>,
        val currentScore: Int,
        val bestScore: Int,
        val isGameOver: Boolean,
        val isUndoable: Boolean,
    ) : GameUiState {

        constructor(
            data: UserData,
            isUndoable: Boolean,
        ) : this(
            gridSize = GRID_SIZE,
            gridTileMovements = data.movements,
            currentScore = data.currentScore,
            bestScore = data.bestScore,
            isGameOver = GameStrategy(GRID_SIZE).checkIsGameOver(data.movements),
            isUndoable = isUndoable,
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
    ) { GamePresenter(gameState.gameRepository, gameState.mutableStack, coroutineScope, eventBus) }
}

class GamePresenter(
    private val gameRepository: GameRepository,
    private val mutableStack: MutableList<UserData>,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    eventBus: EventBus<GameUiEvent> = buildEventBus(),
) : Presenter<GameUiEvent, GameUiState> by buildPresenter(GameUiState.Nothing, eventBus), RememberObserver {

    private var job: Job? = null

    private suspend fun save(data: UserData) {
        if (!GameStrategy(GRID_SIZE).checkIsGameOver(data.movements)) {
            gameRepository.update(data)
        }
    }

    private suspend fun startNewGame(gridSize: Int, bestScore: Int): UserData {
        val updatedData = GameStrategy(gridSize).startNewGame(bestScore)
        mutableStack.clear()
        mutableStack.add(updatedData)
        save(updatedData)
        return updatedData
    }

    private fun checkIsUndoable(): Boolean {
        return mutableStack.size > 1
    }

    override suspend fun handleEvent(event: GameUiEvent) {
        val currentData = mutableStack.lastOrNull()
        when (event) {
            GameUiEvent.Load -> {
                if (currentData != null && currentData.movements.isNotEmpty()) {
                    produceUiState(GameUiState.Success(currentData, checkIsUndoable()))
                } else {
                    produceUiState(GameUiState.Loading)
                    val store = gameRepository.fetch()
                    if (store != UserData() && store.movements.isNotEmpty()) {
                        // Restore a previously saved game.
                        mutableStack.clear()
                        mutableStack.add(store)
                        produceUiState(GameUiState.Success(store, false))
                    } else {
                        produceUiState(GameUiState.Success(startNewGame(GRID_SIZE, 0), false))
                    }
                }
            }
            is GameUiEvent.Move -> {
                if (currentData != null) {
                    val updatedData = with(currentData) {
                        GameStrategy(GRID_SIZE).move(event.direction, movements, currentScore, bestScore)
                    }
                    if (updatedData != null) {
                        // Push game data to stack.
                        mutableStack.add(updatedData)
                        save(updatedData)
                        produceUiState(GameUiState.Success(updatedData, checkIsUndoable()))
                    }
                }
            }
            GameUiEvent.StartNewGame -> {
                val bestScore = currentData?.bestScore ?: 0
                produceUiState(GameUiState.Success(startNewGame(GRID_SIZE, bestScore), false))
            }
            GameUiEvent.Undo -> {
                if (checkIsUndoable()) {
                    // Pop and restore game from stack.
                    val updatedData = with(mutableStack) {
                        removeAt(lastIndex)
                        last()
                    }
                    save(updatedData)
                    produceUiState(GameUiState.Success(updatedData, checkIsUndoable()))
                }
            }
        }
    }

    private fun stop() {
        job?.cancel()
        job = null
    }

    override fun onRemembered() {
        job = coroutineScope.launch {
            handleEvent(GameUiEvent.Load)
        }
    }

    override fun onForgotten() {
        stop()
    }

    override fun onAbandoned() {
        stop()
    }
}
