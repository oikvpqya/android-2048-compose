package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.ui.gamePresenter
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
fun App(repository: GameRepository) {
    val eventFlow = remember { MutableSharedFlow<GameUiEvent>(extraBufferCapacity = 20) }
    AppTheme {
        Surface {
            GameUi(
                uiState = gamePresenter(eventFlow = eventFlow, gameRepository = repository),
                produceEvent = { eventFlow.tryEmit(it) },
            )
        }
    }
}
