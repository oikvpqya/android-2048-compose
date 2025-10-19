package com.alexjlockwood.twentyfortyeight.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PresenterImpl<EVENT, STATE>(
    base: EventBus<EVENT>,
    initialUiState: STATE,
) : Presenter<EVENT, STATE>, EventBus<EVENT> by base {

    private val mutableUiStateFlow = MutableStateFlow(initialUiState)
    override val stateFlow = mutableUiStateFlow.asStateFlow()

    override fun produceUiState(uiState: STATE) {
        mutableUiStateFlow.value = uiState
    }

    override suspend fun handleEvent(event: EVENT) {}
}
