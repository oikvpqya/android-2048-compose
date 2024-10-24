import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import net.harawata.appdirs.AppDirsFactory
import java.nio.file.Files
import java.nio.file.Paths

private const val PACKAGE_NAME = "com.alexjlockwood.twentyfortyeightcompose"
private const val VERSION = "1.0.0"
private const val AUTHOR = "alexjlockwood"

fun main() {
    val repository by lazy {
        val userDataDir = Paths.get(AppDirsFactory.getInstance().getUserDataDir(PACKAGE_NAME, VERSION, AUTHOR))
        if (!Files.exists(userDataDir)) {
            Files.createDirectories(userDataDir)
        }
        DefaultGameRepository(
            store = storeOf(
                file = Path(userDataDir.resolve(USER_DATA_FILE_NAME).toString()),
                default = UserData.EMPTY_USER_DATA,
            ),
        )
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "2048 Compose",
        ) {
            App(repository = repository)
        }
    }
}
