package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.alexjlockwood.twentyfortyeight.runtime.collectAsState
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.ui.rememberGamePresenter

@Composable
fun App() {
    val presenter = rememberGamePresenter()
    AppTheme {
        Surface {
            GameUi(
                uiState = presenter.collectAsState().value,
                produceEvent = presenter::produceEvent,
            )
        }
    }
}
