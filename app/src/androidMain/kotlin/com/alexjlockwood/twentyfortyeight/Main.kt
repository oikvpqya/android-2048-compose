package com.alexjlockwood.twentyfortyeight

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alexjlockwood.twentyfortyeight.domain.DEFAULT_JSON
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.io.files.Path

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = createStatusBarStyle())
        setContent {
            App(repository = (application as GameRepositoryProvider).gameRepository)
        }
    }

    private fun createStatusBarStyle(
        darkScrim: Int = -13615201, // ARGB 0xff303f9f
        lightScrim: Int = -16737844, // ARGB 0xff0099cc
        isDarkTheme: Boolean = Build.VERSION.SDK_INT >= 30 && resources.configuration.isNightModeActive,
    ): SystemBarStyle {
        return if (isDarkTheme) {
            SystemBarStyle.dark(darkScrim)
        } else {
            SystemBarStyle.light(lightScrim, darkScrim)
        }
    }
}

class MainApplication : Application(), GameRepositoryProvider {

    override val gameRepository by lazy {
        DefaultGameRepository(
            store = storeOf(
                file = Path(this.filesDir.absolutePath, USER_DATA_FILE_NAME),
                default = UserData.EMPTY_USER_DATA,
                enableCache = true,
                json = DEFAULT_JSON,
            ),
            dispatcher = Dispatchers.IO + Job(),
        )
    }
}

private interface GameRepositoryProvider {

    val gameRepository: GameRepository
}
