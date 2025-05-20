package com.alexjlockwood.twentyfortyeight.runtime

import kotlinx.coroutines.flow.SharedFlow

interface EventBus<EVENT> {

    val eventFlow: SharedFlow<EVENT>
    fun produceEvent(event: EVENT)
}
