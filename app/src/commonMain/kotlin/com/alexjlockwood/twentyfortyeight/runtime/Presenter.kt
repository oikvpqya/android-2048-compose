package com.alexjlockwood.twentyfortyeight.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

interface Presenter<EVENT, STATE> : EventBus<EVENT> {

    val uiStateFlow: StateFlow<STATE>
    suspend fun handleEvent(event: EVENT)

    @Composable
    fun collectAsUiState(): State<STATE> {
        LaunchedEffect(this) { eventFlow.collect(::handleEvent) }
        return uiStateFlow.collectAsState()
    }
}
