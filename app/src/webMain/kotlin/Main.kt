import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.repository.LocalGameRepository
import com.alexjlockwood.twentyfortyeight.repository.WebGameRepository
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val gameRepository by lazy {
        WebGameRepository()
    }
    ComposeViewport(viewportContainerId = "app") {
        CompositionLocalProvider(
            LocalGameRepository provides gameRepository,
        ) {
            App()
        }
        LaunchedEffect(Unit) {
            document.getElementById("indicator")?.let { element ->
                (element as HTMLDivElement).style.display = "none"
            }
        }
    }
}
