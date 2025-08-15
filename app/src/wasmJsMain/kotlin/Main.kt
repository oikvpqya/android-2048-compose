import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val repository by lazy {
        DefaultGameRepository()
    }
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        App(repository = repository)
        LaunchedEffect(Unit) {
            document.getElementById("indicator")?.let { element ->
                (element as HTMLDivElement).style.display = "none"
            }
        }
    }
}
