package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.alexjlockwood.twentyfortyeight.repository.GameUseCase
import com.alexjlockwood.twentyfortyeight.runtime.collectAsUiState
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.ui.rememberGamePresenter

@Composable
fun App(gameUseCase: GameUseCase) {
    val presenter = rememberGamePresenter(gameUseCase = gameUseCase)
    AppTheme {
        Surface {
            GameUi(
                uiState = presenter.collectAsUiState().value,
                produceEvent = presenter::produceEvent,
            )
        }
    }
}
