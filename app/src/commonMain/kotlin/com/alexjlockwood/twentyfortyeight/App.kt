package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.ui.rememberEventBus
import com.alexjlockwood.twentyfortyeight.ui.rememberGamePresenter

@Composable
fun App(repository: GameRepository) {
    val presenter = rememberGamePresenter(gameRepository = repository)
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
