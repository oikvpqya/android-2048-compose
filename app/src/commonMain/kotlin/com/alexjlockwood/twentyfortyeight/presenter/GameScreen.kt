package com.alexjlockwood.twentyfortyeight.presenter

import CommonParcelize
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen

@CommonParcelize
data object GameScreen : Screen {
    data class State(
        val gridTileMovements: List<GridTileMovement>,
        val currentScore: Int,
        val bestScore: Int,
        val isGameOver: Boolean,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class Move(val direction: Direction) : Event
        data object StartNewGame : Event
    }
}
