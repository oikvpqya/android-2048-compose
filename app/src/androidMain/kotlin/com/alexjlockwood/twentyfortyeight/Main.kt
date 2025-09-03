package com.alexjlockwood.twentyfortyeight

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import com.alexjlockwood.twentyfortyeight.ui.GameUseCase
import kotlin.io.path.Path

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = createStatusBarStyle())
        setContent {
            App(gameUseCase = (application as GameUseCaseProvider).gameUseCase)
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

class MainApplication : Application(), GameUseCaseProvider {

    override val gameUseCase by lazy {
        GameUseCase(
            gameRepository = DefaultGameRepository(
                file = Path(filesDir.absolutePath, USER_DATA_FILE_NAME),
            ),
        )
    }
}

private interface GameUseCaseProvider {

    val gameUseCase: GameUseCase
}
