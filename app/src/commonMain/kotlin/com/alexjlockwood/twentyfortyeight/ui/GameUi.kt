package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.alexjlockwood.twentyfortyeight.domain.Direction
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Renders the 2048 game's home screen UI.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameUi(
    uiState: GameUiState,
    modifier: Modifier = Modifier,
    produceEvent: (GameUiEvent) -> Unit,
) {
    var shouldShowAboutDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowNewGameDialog by rememberSaveable { mutableStateOf(false) }
    BackHandler(uiState is GameUiState.Success && uiState.canUndo) { produceEvent(GameUiEvent.Undo) }
    Column(
        modifier = modifier
            .safeDrawingPadding()
            .fillMaxSize(),
    ) {
        TopAppBar(
            title = { Text(text = "2048 Compose") },
            contentColor = Color.White,
            backgroundColor = MaterialTheme.colors.primaryVariant,
            actions = {
                IconButton(
                    onClick = { produceEvent(GameUiEvent.Undo) },
                    enabled = uiState is GameUiState.Success && uiState.canUndo,
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                IconButton(
                    onClick = { shouldShowNewGameDialog = true },
                    enabled = uiState is GameUiState.Success,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                }
                IconButton(
                    onClick = { shouldShowAboutDialog = true },
                ) {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                }
            },
        )
        if (uiState !is GameUiState.Success) {
            return@Column
        }
        GameLayout(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var swipeAngle = 0.0
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            swipeAngle = with(dragAmount) { (atan2(-y, x) * 180 / PI + 360) % 360 }
                        },
                        onDragEnd = {
                            val direction = when {
                                45 <= swipeAngle && swipeAngle < 135 -> Direction.NORTH
                                135 <= swipeAngle && swipeAngle < 225 -> Direction.WEST
                                225 <= swipeAngle && swipeAngle < 315 -> Direction.SOUTH
                                else -> Direction.EAST
                            }
                            produceEvent(GameUiEvent.Move(direction = direction))
                        }
                    )
                },
            gameGrid = { GameGrid(gridTileMovements = uiState.gridTileMovements, gridSize = it) },
            currentScoreText = { TextLabel(text = "${uiState.currentScore}", fontSize = 36.sp) },
            currentScoreLabel = { TextLabel(text = "Score", fontSize = 18.sp) },
            bestScoreText = { TextLabel(text = "${uiState.bestScore}", fontSize = 36.sp) },
            bestScoreLabel = { TextLabel(text = "Best", fontSize = 18.sp) },
        )
    }
    if (uiState is GameUiState.Success && uiState.isGameOver) {
        GameDialog(
            title = "Game over",
            message = "Start a new game?",
            onConfirmListener = { produceEvent(GameUiEvent.StartNewGame) },
            onDismissListener = {
                // TODO: allow user to dismiss the dialog so they can take a screenshot
                produceEvent(GameUiEvent.Undo)
            },
        )
    } else if (shouldShowNewGameDialog) {
        GameDialog(
            title = "Start a new game?",
            message = "Starting a new game will erase your current game",
            onConfirmListener = {
                produceEvent(GameUiEvent.StartNewGame)
                shouldShowNewGameDialog = false
            },
            onDismissListener = {
                shouldShowNewGameDialog = false
            },
            confirmEnabled = uiState is GameUiState.Success,
        )
    } else if (shouldShowAboutDialog) {
        AboutDialog(
            onDismissListener = {
                shouldShowAboutDialog = false
            },
        )
    }
    LaunchedEffect(Unit) {
        when (uiState) {
            GameUiState.Loading, is GameUiState.Success -> Unit
            GameUiState.Nothing -> {
                produceEvent(GameUiEvent.Load)
            }
        }
    }
}

@Composable
private fun TextLabel(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = FontWeight.Light,
    )
}
