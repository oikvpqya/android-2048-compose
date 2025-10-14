package com.alexjlockwood.twentyfortyeight.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

interface Presenter<EVENT, STATE> : EventBus<EVENT> {

    val uiStateFlow: StateFlow<STATE>
    fun produceUiState(uiState: STATE)
    fun handleEvent(event: EVENT)
}

@Composable
fun <EVENT, STATE> Presenter<EVENT, STATE>.collectAsUiState(): State<STATE> {
    LaunchedEffect(this) {
        eventFlow.collect { event ->
            handleEvent(event)
        }
    }
    return uiStateFlow.collectAsState()
}

fun <EVENT, STATE> buildPresenter(
    initialUiState: STATE,
    eventBus: EventBus<EVENT> = buildEventBus(),
): Presenter<EVENT, STATE> {
    return PresenterImpl(eventBus, initialUiState)
}
