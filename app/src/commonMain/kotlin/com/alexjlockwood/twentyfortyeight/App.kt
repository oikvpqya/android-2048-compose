package com.alexjlockwood.twentyfortyeight

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.runtime.Navigator

@Composable
fun App(backStack: SaveableBackStack, navigator: Navigator) {
    AppTheme {
        Surface {
            NavigableCircuitContent(
                navigator = navigator,
                backStack = backStack,
            )
        }
    }
}
