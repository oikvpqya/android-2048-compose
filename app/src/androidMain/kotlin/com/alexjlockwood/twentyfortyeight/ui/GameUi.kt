package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.alexjlockwood.twentyfortyeight.R
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Renders the 2048 game's home screen UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameUi(
    gridTileMovements: List<GridTileMovement>,
    currentScore: Int,
    bestScore: Int,
    moveCount: Int,
    canUndo: Boolean,
    isGameOver: Boolean,
    isDarkTheme: Boolean,
    isPortrait: Boolean,
    onNewGameRequested: () -> Unit,
    onUndoGameRequested: () -> Unit,
    onSwipeListener: (direction: Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowNewGameDialog by remember { mutableStateOf(false) }
    var swipeAngle by remember { mutableDoubleStateOf(0.0) }
    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { onUndoGameRequested() }, enabled = canUndo) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                    IconButton(onClick = { shouldShowNewGameDialog = true }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        GameLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            swipeAngle = with(dragAmount) { (atan2(-y, x) * 180 / PI + 360) % 360 }
                        },
                        onDragEnd = {
                            onSwipeListener(
                                when {
                                    45 <= swipeAngle && swipeAngle < 135 -> Direction.NORTH
                                    135 <= swipeAngle && swipeAngle < 225 -> Direction.WEST
                                    225 <= swipeAngle && swipeAngle < 315 -> Direction.SOUTH
                                    else -> Direction.EAST
                                }
                            )
                        }
                    )
                },
            isPortrait = isPortrait,
            gameGrid = { BoxWithConstraints { GameGrid(gridTileMovements, moveCount, isDarkTheme, gridSize = min(maxWidth, maxHeight)) } },
            currentScoreText = { TextLabel(text = "$currentScore", fontSize = 36.sp) },
            currentScoreLabel = { TextLabel(text = "Score", fontSize = 18.sp) },
            bestScoreText = { TextLabel(text = "$bestScore", fontSize = 36.sp) },
            bestScoreLabel = { TextLabel(text = "Best", fontSize = 18.sp) },
        )
    }
    if (isGameOver) {
        GameDialog(
            title = "Game over",
            message = "Start a new game?",
            onConfirmListener = { onNewGameRequested() },
            onDismissListener = { onUndoGameRequested() },
        )
    } else if (shouldShowNewGameDialog) {
        GameDialog(
            title = "Start a new game?",
            message = "Starting a new game will erase your current game",
            onConfirmListener = {
                onNewGameRequested()
                shouldShowNewGameDialog = false
            },
            onDismissListener = {
                shouldShowNewGameDialog = false
            },
        )
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

@Composable
private fun GameLayout(
    isPortrait: Boolean,
    gameGrid: @Composable (() -> Unit),
    currentScoreText: @Composable (() -> Unit),
    currentScoreLabel: @Composable (() -> Unit),
    bestScoreText: @Composable (() -> Unit),
    bestScoreLabel: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
) {
    if (isPortrait) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { gameGrid() }
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    currentScoreText()
                    currentScoreLabel()
                }
                Column(horizontalAlignment = Alignment.End) {
                    bestScoreText()
                    bestScoreLabel()
                }
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) { gameGrid() }
            Column {
                currentScoreText()
                currentScoreLabel()
                bestScoreText()
                bestScoreLabel()
            }
        }
    }
}
