package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexjlockwood.twentyfortyeight.domain.Cell
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTile
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

const val GRID_SIZE = 4
private const val NUM_INITIAL_TILES = 2
private val EMPTY_GRID = (0 until GRID_SIZE).map { arrayOfNulls<Tile?>(GRID_SIZE).toList() }

sealed interface GameUiEvent {

    data object Load : GameUiEvent
    data class Move(val direction: Direction) : GameUiEvent
    data object StartNewGame : GameUiEvent
    data object Undo : GameUiEvent
}

sealed interface GameUiState {

    data object Loading : GameUiState
    data object Nothing : GameUiState
    data class Success(
        val gridTileMovements: List<GridTileMovement>,
        val currentScore: Int,
        val bestScore: Int,
        val isGameOver: Boolean,
        val canUndo: Boolean,
    ) : GameUiState
}

/**
 * Presenter that contains the logic that powers the 2048 game.
 */
class GamePresenter(
    private val gameRepository: GameRepository,
) : ViewModel() {

    private var grid = EMPTY_GRID
    private var gridTileMovements = emptyList<GridTileMovement>()
    private var currentScore = 0
    private var bestScore = 0
    private var isGameOver = false
    private var moveCount = 0
    private var canUndo = false
    private val stack = ArrayDeque<UserData>(emptyList())

    private suspend fun save() {
        if (!isGameOver) {
            withContext(viewModelScope.coroutineContext) {
                launch { gameRepository.update(grid, currentScore, bestScore) }
            }
        }
    }

    private suspend fun startNewGame(
        updateUiStete: (GameUiState) -> Unit,
    ) {
        gridTileMovements = (0 until NUM_INITIAL_TILES).mapNotNull { createRandomAddedTile(EMPTY_GRID) }
        val addedGridTiles = gridTileMovements.map { it.toGridTile }
        grid = EMPTY_GRID.map { row, col, _ -> addedGridTiles.find { row == it.cell.row && col == it.cell.col }?.tile }
        currentScore = 0
        isGameOver = false
        moveCount = 0
        stack.clear()
        canUndo = false
        save()
        updateUiStete(GameUiState.Success(gridTileMovements, currentScore, bestScore, isGameOver, canUndo))
    }

    private suspend fun move(
        direction: Direction,
        updateUiStete: (GameUiState) -> Unit,
    ) {
        var (updatedGrid, updatedGridTileMovements) = makeMove(grid, direction)

        if (!hasGridChanged(updatedGridTileMovements)) {
            // No tiles were moved.
            return
        }

        // Push game data to stack.
        stack.add(UserData(grid, currentScore, bestScore))

        // Increment the score.
        val scoreIncrement = updatedGridTileMovements.filter { it.fromGridTile == null }.sumOf { it.toGridTile.tile.num }
        currentScore += scoreIncrement
        bestScore = max(bestScore, currentScore)

        // Attempt to add a new tile to the grid.
        updatedGridTileMovements = updatedGridTileMovements.toMutableList()
        val addedTileMovement = createRandomAddedTile(updatedGrid)
        if (addedTileMovement != null) {
            val (cell, tile) = addedTileMovement.toGridTile
            updatedGrid = updatedGrid.map { r, c, it -> if (cell.row == r && cell.col == c) tile else it }
            updatedGridTileMovements.add(addedTileMovement)
        }

        grid = updatedGrid
        gridTileMovements = updatedGridTileMovements.sortedWith { a, _ -> if (a.fromGridTile == null) 1 else -1 }
        isGameOver = checkIsGameOver(grid)
        moveCount++
        canUndo = true
        save()
        updateUiStete(GameUiState.Success(gridTileMovements, currentScore, bestScore, isGameOver, canUndo))
    }

    private suspend fun undo(
        updateUiStete: (GameUiState) -> Unit,
    ) {
        require(canUndo)
        // Pop and restore game from stack.
        val (updatedGrid, updatedCurrentScore, updatedBestScore) = stack.removeLast()
        grid = updatedGrid ?: EMPTY_GRID
        gridTileMovements = grid.toGridTileMovements()
        currentScore = updatedCurrentScore
        bestScore = updatedBestScore
        isGameOver = checkIsGameOver(grid)
        moveCount--
        canUndo = stack.isNotEmpty()
        save()
        updateUiStete(GameUiState.Success(gridTileMovements, currentScore, bestScore, isGameOver, canUndo))
    }

    private suspend fun load(
        updateUiStete: (GameUiState) -> Unit,
    ) {
        if (grid != EMPTY_GRID) {
            updateUiStete(GameUiState.Success(gridTileMovements, currentScore, bestScore, isGameOver, canUndo))
            return
        }
        updateUiStete(GameUiState.Loading)
        val userData = withContext(viewModelScope.coroutineContext) {
            gameRepository.fetch()
        }
        bestScore = userData.bestScore
        if (userData.grid == null) {
            startNewGame(updateUiStete)
        } else {
            // Restore a previously saved game.
            grid = userData.grid
            gridTileMovements = userData.grid.toGridTileMovements()
            currentScore = userData.currentScore
            isGameOver = checkIsGameOver(userData.grid)
        }
        updateUiStete(GameUiState.Success(gridTileMovements, currentScore, bestScore, isGameOver, canUndo))
    }

    @Composable
    fun uiState(
        eventFlow: Flow<GameUiEvent>,
    ): GameUiState {
        var uiState by remember { mutableStateOf<GameUiState>(GameUiState.Nothing) }
        LaunchedEffect(eventFlow) {
            eventFlow.collect { event ->
                when (event) {
                    GameUiEvent.Load -> {
                        load { uiState = it }
                    }
                    is GameUiEvent.Move -> {
                        move(event.direction) { uiState = it }
                    }
                    GameUiEvent.StartNewGame -> {
                        startNewGame { uiState = it }
                    }
                    GameUiEvent.Undo -> {
                        undo { uiState = it }
                    }
                }
            }
        }
        return uiState
    }
}

private fun createRandomAddedTile(grid: List<List<Tile?>>): GridTileMovement? {
    val emptyCells = grid.flatMapIndexed { row, tiles ->
        tiles.mapIndexed { col, it -> if (it == null) Cell(row, col) else null }.filterNotNull()
    }
    val emptyCell = emptyCells.getOrNull(emptyCells.indices.random()) ?: return null
    return GridTileMovement.add(GridTile(emptyCell, if (Random.nextFloat() < 0.9f) Tile(2) else Tile(4)))
}

private fun makeMove(grid: List<List<Tile?>>, direction: Direction): Pair<List<List<Tile?>>, List<GridTileMovement>> {
    val numRotations = when (direction) {
        Direction.WEST -> 0
        Direction.SOUTH -> 1
        Direction.EAST -> 2
        Direction.NORTH -> 3
    }

    // Rotate the grid so that we can process it as if the user has swiped their
    // finger from right to left.
    var updatedGrid = grid.rotate(numRotations)

    val gridTileMovements = mutableListOf<GridTileMovement>()

    updatedGrid = List(updatedGrid.size) { currentRowIndex ->
        val tiles = updatedGrid[currentRowIndex].toMutableList()
        var lastSeenTileIndex: Int? = null
        var lastSeenEmptyIndex: Int? = null
        for (currentColIndex in tiles.indices) {
            val currentTile = tiles[currentColIndex]
            if (currentTile == null) {
                // We are looking at an empty cell in the grid.
                if (lastSeenEmptyIndex == null) {
                    // Keep track of the first empty index we find.
                    lastSeenEmptyIndex = currentColIndex
                }
                continue
            }

            // Otherwise, we have encountered a tile that could either be shifted,
            // merged, or not moved at all.
            val currentGridTile = GridTile(getRotatedCellAt(currentRowIndex, currentColIndex, numRotations), currentTile)

            if (lastSeenTileIndex == null) {
                // This is the first tile in the list that we've found.
                if (lastSeenEmptyIndex == null) {
                    // Keep the tile at its same location.
                    gridTileMovements.add(GridTileMovement.noop(currentGridTile))
                    lastSeenTileIndex = currentColIndex
                } else {
                    // Shift the tile to the location of the furthest empty cell in the list.
                    val targetCell = getRotatedCellAt(currentRowIndex, lastSeenEmptyIndex, numRotations)
                    val targetGridTile = GridTile(targetCell, currentTile)
                    gridTileMovements.add(GridTileMovement.shift(currentGridTile, targetGridTile))

                    tiles[lastSeenEmptyIndex] = currentTile
                    tiles[currentColIndex] = null
                    lastSeenTileIndex = lastSeenEmptyIndex
                    lastSeenEmptyIndex++
                }
            } else {
                // There is a previous tile in the list that we need to process.
                if (tiles[lastSeenTileIndex]!!.num == currentTile.num) {
                    // Shift the tile to the location where it will be merged.
                    val targetCell = getRotatedCellAt(currentRowIndex, lastSeenTileIndex, numRotations)
                    gridTileMovements.add(GridTileMovement.shift(currentGridTile, GridTile(targetCell, currentTile)))

                    // Merge the current tile with the previous tile.
                    val addedTile = currentTile * 2
                    gridTileMovements.add(GridTileMovement.add(GridTile(targetCell, addedTile)))

                    tiles[lastSeenTileIndex] = addedTile
                    tiles[currentColIndex] = null
                    lastSeenTileIndex = null
                    if (lastSeenEmptyIndex == null) {
                        lastSeenEmptyIndex = currentColIndex
                    }
                } else {
                    if (lastSeenEmptyIndex == null) {
                        // Keep the tile at its same location.
                        gridTileMovements.add(GridTileMovement.noop(currentGridTile))
                    } else {
                        // Shift the current tile towards the previous tile.
                        val targetCell = getRotatedCellAt(currentRowIndex, lastSeenEmptyIndex, numRotations)
                        val targetGridTile = GridTile(targetCell, currentTile)
                        gridTileMovements.add(GridTileMovement.shift(currentGridTile, targetGridTile))

                        tiles[lastSeenEmptyIndex] = currentTile
                        tiles[currentColIndex] = null
                        lastSeenEmptyIndex++
                    }
                    lastSeenTileIndex++
                }
            }
        }
        tiles
    }

    // Rotate the grid back to its original state.
    updatedGrid = updatedGrid.rotate((-numRotations).floorMod(Direction.entries.size))

    return Pair(updatedGrid, gridTileMovements)
}

private fun <T> List<List<T>>.rotate(numRotations: Int): List<List<T>> {
    return map { row, col, _ ->
        val (rotatedRow, rotatedCol) = getRotatedCellAt(row, col, numRotations)
        this[rotatedRow][rotatedCol]
    }
}

private fun getRotatedCellAt(row: Int, col: Int, numRotations: Int): Cell {
    return when (numRotations) {
        0 -> Cell(row, col)
        1 -> Cell(GRID_SIZE - 1 - col, row)
        2 -> Cell(GRID_SIZE - 1 - row, GRID_SIZE - 1 - col)
        3 -> Cell(col, GRID_SIZE - 1 - row)
        else -> throw IllegalArgumentException("numRotations must be an integer in [0,3]")
    }
}

private fun <T> List<List<T>>.map(transform: (row: Int, col: Int, T) -> T): List<List<T>> {
    return mapIndexed { row, rowTiles -> rowTiles.mapIndexed { col, it -> transform(row, col, it) } }
}

private fun checkIsGameOver(grid: List<List<Tile?>>): Boolean {
    // The game is over if no tiles can be moved in any of the 4 directions.
    return Direction.entries.toList().none { hasGridChanged(makeMove(grid, it).second) }
}

private fun hasGridChanged(gridTileMovements: List<GridTileMovement>): Boolean {
    // The grid has changed if any of the tiles have moved to a different location.
    return gridTileMovements.any {
        val (fromTile, toTile) = it
        fromTile == null || fromTile.cell != toTile.cell
    }
}

private fun Int.floorMod(other: Int): Int {
    val mod = this % other
    return if ((mod xor other) < 0 && mod != 0) mod + other else mod
}

private fun List<List<Tile?>>.toGridTileMovements(): List<GridTileMovement> {
    return flatMapIndexed { row, tiles ->
        tiles.mapIndexed { col, tile ->
            GridTileMovement.noop(GridTile(Cell(row, col), tile ?: return@mapIndexed null))
        }
    }.filterNotNull()
}
