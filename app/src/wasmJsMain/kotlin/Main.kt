import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import com.alexjlockwood.twentyfortyeight.ui.GameUseCase
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val gameUseCase by lazy {
        GameUseCase(gameRepository = DefaultGameRepository())
    }
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App(gameUseCase = gameUseCase)
        LaunchedEffect(Unit) {
            document.getElementById("indicator")?.let { element ->
                (element as HTMLDivElement).style.display = "none"
            }
        }
    }
}
