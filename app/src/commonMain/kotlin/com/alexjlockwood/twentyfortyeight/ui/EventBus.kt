package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface EventBus<EVENT> {

    val eventFlow: SharedFlow<EVENT>
    fun produceEvent(event: EVENT)
}

class DefaultEventBus<EVENT> : EventBus<EVENT> {

    private val mutableEventFlow = MutableSharedFlow<EVENT>(extraBufferCapacity = 20)
    override val eventFlow = mutableEventFlow.asSharedFlow()

    override fun produceEvent(event: EVENT) {
        mutableEventFlow.tryEmit(event)
    }
}

@Composable
fun <EVENT> rememberEventBus(): EventBus<EVENT> {
    return remember { DefaultEventBus() }
}
