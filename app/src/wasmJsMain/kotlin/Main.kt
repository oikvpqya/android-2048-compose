import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.presenter.GamePresenter
import com.alexjlockwood.twentyfortyeight.presenter.GameScreen
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import com.alexjlockwood.twentyfortyeight.ui.GameUiRoot
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import okio.Path.Companion.toPath

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val store = getStore(USER_DATA_FILE_NAME.toPath())
    val circuit: Circuit = Circuit.Builder()
        .addPresenterFactory(GamePresenter.Factory(GameRepository(store)))
        .addUi<GameScreen, GameScreen.State> { state, modifier -> GameUiRoot(state, modifier) }
        .build()

    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        val backStack = rememberSaveableBackStack(GameScreen)
        val navigator = rememberCircuitNavigator(backStack) {}
        CircuitCompositionLocals(circuit) {
            App(
                backStack = backStack,
                navigator = navigator,
            )
        }
    }
}
