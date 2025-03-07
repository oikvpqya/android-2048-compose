package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.GamePresenter
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.ui.rememberEventBus

@Composable
fun App(repository: GameRepository) {
    val presenter = remember(repository) { GamePresenter(gameRepository = repository) }
    val eventBus = rememberEventBus<GameUiEvent>()
    AppTheme {
        Surface {
            GameUi(
                uiState = presenter.uiState(eventFlow = eventBus.eventFlow),
                produceEvent = { eventBus.produceEvent(it) },
            )
        }
    }
}
