package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.GamePresenter
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
fun App(repository: GameRepository) {
    val eventFlow = remember { MutableSharedFlow<GameUiEvent>(extraBufferCapacity = 20) }
    val presenter = viewModel<GamePresenter>(
        factory = remember(repository) { viewModelFactory { initializer { GamePresenter(gameRepository = repository) } } },
    )
    AppTheme {
        Surface {
            GameUi(
                uiState = presenter.uiState(eventFlow = eventFlow),
                produceEvent = { eventFlow.tryEmit(it) },
            )
        }
    }
}
