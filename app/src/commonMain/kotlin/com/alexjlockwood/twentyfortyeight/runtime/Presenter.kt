package com.alexjlockwood.twentyfortyeight.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface Presenter<EVENT, STATE> : EventBus<EVENT> {

    val uiStateFlow: StateFlow<STATE>
    fun produceUiState(uiState: STATE)
    fun handleEvent(event: EVENT, coroutineContext: CoroutineContext = EmptyCoroutineContext)
}

@Composable
fun <EVENT, STATE> Presenter<EVENT, STATE>.collectAsUiState(): State<STATE> {
    LaunchedEffect(this) {
        eventFlow.collect { event ->
            handleEvent(event, coroutineContext)
        }
    }
    return uiStateFlow.collectAsState()
}

fun <EVENT, STATE> buildPresenter(
    initialUiState: STATE,
): Presenter<EVENT, STATE> {
    return PresenterImpl(EventBusImpl(), initialUiState)
}
