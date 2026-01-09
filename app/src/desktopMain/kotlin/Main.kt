import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.repository.GameState
import com.alexjlockwood.twentyfortyeight.repository.JvmAndroidGameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import net.harawata.appdirs.AppDirsFactory
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists

private const val PACKAGE_NAME = "com.alexjlockwood.twentyfortyeightcompose"
private const val VERSION = "1.0.0"
private const val AUTHOR = "alexjlockwood"

fun main() {
    val gameState by lazy {
        val userDataRootDirString = AppDirsFactory.getInstance().getUserDataDir(null, null, null) ?: ""
        val userDataDir = Path(userDataRootDirString, AUTHOR, PACKAGE_NAME, VERSION)
        if (!userDataDir.exists()) {
            userDataDir.createDirectories()
        }
        val gameRepository = JvmAndroidGameRepository(
            file = userDataDir.div(USER_DATA_FILE_NAME),
        )
        GameState(gameRepository = gameRepository)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "2048 Compose",
        ) {
            App(gameState = gameState)
        }
    }
}
