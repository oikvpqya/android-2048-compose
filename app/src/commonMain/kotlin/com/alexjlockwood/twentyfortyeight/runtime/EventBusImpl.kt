package com.alexjlockwood.twentyfortyeight.runtime

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EventBusImpl<EVENT> : EventBus<EVENT> {

    private val mutableEventFlow = MutableSharedFlow<EVENT>(extraBufferCapacity = 20)
    override val eventFlow = mutableEventFlow.asSharedFlow()

    override fun produceEvent(event: EVENT) {
        mutableEventFlow.tryEmit(event)
    }
}
