package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.runtime.collectAsUiState
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.ui.rememberGamePresenter

@Composable
fun App(repository: GameRepository) {
    val presenter = rememberGamePresenter(gameRepository = repository)
    AppTheme {
        Surface {
            GameUi(
                uiState = presenter.collectAsUiState().value,
                produceEvent = presenter::produceEvent,
            )
        }
    }
}
