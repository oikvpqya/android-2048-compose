package com.alexjlockwood.twentyfortyeight.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.SharedFlow

interface EventBus<EVENT> {

    val eventFlow: SharedFlow<EVENT>
    fun produceEvent(event: EVENT)
}

@Composable
fun <EVENT> rememberEventBus(): EventBus<EVENT> {
    return remember {
        buildEventBus()
    }
}

fun <EVENT> buildEventBus(): EventBus<EVENT> {
    return EventBusImpl()
}
