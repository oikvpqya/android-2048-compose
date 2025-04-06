package com.alexjlockwood.twentyfortyeight.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface EventBus<EVENT> {

    val eventFlow: SharedFlow<EVENT>
    fun produceEvent(event: EVENT)
}

class EventBusImpl<EVENT> : EventBus<EVENT> {

    private val mutableEventFlow = MutableSharedFlow<EVENT>(extraBufferCapacity = 20)
    override val eventFlow = mutableEventFlow.asSharedFlow()

    override fun produceEvent(event: EVENT) {
        mutableEventFlow.tryEmit(event)
    }
}
