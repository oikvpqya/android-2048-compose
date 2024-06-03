import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alexjlockwood.twentyfortyeight.App
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import okio.Path.Companion.toPath

fun main() = application {
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = emptyList(),
        produceFile = { USER_DATA_FILE_NAME.toPath() },
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "2048 Compose",
    ) {
        App(
            dataStore = dataStore,
            isPortrait = false
        )
    }
}
