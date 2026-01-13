package com.alexjlockwood.twentyfortyeight

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.GameState
import com.alexjlockwood.twentyfortyeight.repository.JvmAndroidGameRepository
import com.alexjlockwood.twentyfortyeight.repository.LocalGameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import kotlin.io.path.Path

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = createStatusBarStyle())
        setContent {
            CompositionLocalProvider(
                LocalGameRepository provides (application as GameRepositoryProvider).gameRepository,
            ) {
                App(gameState = (application as GameStateProvider).gameState)
            }
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

class MainApplication : Application(), GameRepositoryProvider, GameStateProvider {

    override val gameRepository by lazy {
        JvmAndroidGameRepository(
            file = Path(filesDir.absolutePath, USER_DATA_FILE_NAME),
        )
    }

    override val gameState by lazy {
        GameState()
    }
}

private interface GameRepositoryProvider {

    val gameRepository: GameRepository
}

private interface GameStateProvider {

    val gameState: GameState
}
