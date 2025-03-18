import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import com.alexjlockwood.twentyfortyeight.repository.PolymorphismJson
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import io.github.xxfast.kstore.storage.StorageCodec
import io.github.xxfast.kstore.storage.localStorage
import io.github.xxfast.kstore.storeOf
import kotlinx.browser.document
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val repository by lazy {
        DefaultGameRepository(
            store = storeOf(
                codec = StorageCodec(
                    key = USER_DATA_FILE_NAME,
                    format = PolymorphismJson,
                    serializer = MapSerializer(String.serializer(), PolymorphicSerializer(Any::class)),
                    storage = localStorage,
                ),
                default = null,
                enableCache = false,
            ),
        )
    }
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App(repository = repository)
        LaunchedEffect(Unit) {
            document.getElementById("indicator")?.let { element ->
                (element as HTMLDivElement).style.display = "none"
            }
        }
    }
}
