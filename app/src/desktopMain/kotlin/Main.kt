import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import com.alexjlockwood.twentyfortyeight.presenter.GamePresenter
import com.alexjlockwood.twentyfortyeight.presenter.GameScreen
import com.alexjlockwood.twentyfortyeight.ui.GameUiRoot
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import net.harawata.appdirs.AppDirsFactory
import okio.FileSystem
import okio.Path.Companion.toPath

private const val PACKAGE_NAME = "com.alexjlockwood.twentyfortyeightcompose"
private const val VERSION = "1.0.0"
private const val AUTHOR = "alexjlockwood"

fun main() = application {
    val filesDir = AppDirsFactory.getInstance().getUserDataDir(PACKAGE_NAME, VERSION, AUTHOR).toPath()
    if (!filesDir.toFile().exists()) {
        FileSystem.SYSTEM.createDirectories(filesDir)
    }
    val store = getStore(filesDir.resolve(USER_DATA_FILE_NAME))
    val circuit: Circuit = Circuit.Builder()
        .addPresenterFactory(GamePresenter.Factory(GameRepository(store)))
        .addUi<GameScreen, GameScreen.State> { state, modifier -> GameUiRoot(state, modifier) }
        .build()

    Window(
        onCloseRequest = ::exitApplication,
        title = "2048 Compose",
    ) {
        val backStack = rememberSaveableBackStack(GameScreen)
        val navigator = rememberCircuitNavigator(backStack) { exitApplication() }
        CircuitCompositionLocals(circuit) {
            App(
                backStack = backStack,
                navigator = navigator,
            )
        }
    }
}
