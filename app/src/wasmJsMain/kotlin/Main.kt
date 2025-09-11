import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import com.alexjlockwood.twentyfortyeight.repository.GameState
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val gameState by lazy {
        GameState(gameRepository = DefaultGameRepository())
    }
    ComposeViewport(viewportContainerId = "app") {
        App(gameState = gameState)
        LaunchedEffect(Unit) {
            document.getElementById("indicator")?.let { element ->
                (element as HTMLDivElement).style.display = "none"
            }
        }
    }
}
