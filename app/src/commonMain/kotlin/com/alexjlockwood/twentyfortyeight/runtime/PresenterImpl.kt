package com.alexjlockwood.twentyfortyeight.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PresenterImpl<EVENT, STATE>(
    base: EventBus<EVENT>,
    initialState: STATE,
) : Presenter<EVENT, STATE>, EventBus<EVENT> by base {

    private val mutableStateFlow = MutableStateFlow(initialState)
    override val stateFlow = mutableStateFlow.asStateFlow()

    override fun produceState(state: STATE) {
        mutableStateFlow.value = state
    }

    override suspend fun handleEvent(event: EVENT) {}
}
