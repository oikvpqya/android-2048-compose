package com.alexjlockwood.twentyfortyeight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import com.alexjlockwood.twentyfortyeight.presenter.GamePresenter
import com.alexjlockwood.twentyfortyeight.presenter.GameScreen
import com.alexjlockwood.twentyfortyeight.ui.GameUiRoot
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import getStore
import okio.Path.Companion.toPath

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = getStore(application.filesDir.absolutePath.toPath().resolve(USER_DATA_FILE_NAME))
        val circuit: Circuit = Circuit.Builder()
            .addPresenterFactory(GamePresenter.Factory(GameRepository(store)))
            .addUi<GameScreen, GameScreen.State> { state, modifier -> GameUiRoot(state, modifier) }
            .build()

        setContent {
            val backStack = rememberSaveableBackStack(GameScreen)
            val navigator = rememberCircuitNavigator(backStack)
            CircuitCompositionLocals(circuit) {
                App(
                    backStack = backStack,
                    navigator = navigator,
                )
            }
        }
    }
}
