package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableSharedFlow

class EventBus<EVENT> {

    val eventFlow = MutableSharedFlow<EVENT>(extraBufferCapacity = 20)
    fun produceEvent(event: EVENT) = eventFlow.tryEmit(event)
}

@Composable
fun <EVENT> rememberEventBus(): EventBus<EVENT> {
    return remember { EventBus() }
}
