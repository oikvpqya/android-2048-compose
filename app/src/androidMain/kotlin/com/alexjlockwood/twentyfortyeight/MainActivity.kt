package com.alexjlockwood.twentyfortyeight

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.AppTheme
import com.alexjlockwood.twentyfortyeight.ui.GameUi
import com.alexjlockwood.twentyfortyeight.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VERSION.SDK_INT < VERSION_CODES.O) {
            enableEdgeToEdge()
        } else SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT).also {
            enableEdgeToEdge(it, it)
        }
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val gameViewModel = viewModel { GameViewModel(GameRepository(application)) }
            val isDarkTheme = isSystemInDarkTheme()
            BackHandler(gameViewModel.canUndo) { gameViewModel.undo() }
            AppTheme(isDarkTheme) {
                Surface {
                    GameUi(
                        gridTileMovements = gameViewModel.gridTileMovements,
                        currentScore = gameViewModel.currentScore,
                        bestScore = gameViewModel.bestScore,
                        moveCount = gameViewModel.moveCount,
                        canUndo = gameViewModel.canUndo,
                        isGameOver = gameViewModel.isGameOver,
                        isDarkTheme = isDarkTheme,
                        isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT,
                        onNewGameRequested = { gameViewModel.startNewGame() },
                        onUndoGameRequested = { gameViewModel.undo() },
                        onSwipeListener = { direction -> gameViewModel.move(direction) },
                    )
                }
            }
        }
    }
}
