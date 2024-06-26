package com.alexjlockwood.twentyfortyeight.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.alexjlockwood.twentyfortyeight.domain.Cell
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTile
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

const val GRID_SIZE = 4
private const val NUM_INITIAL_TILES = 2
private val EMPTY_GRID = (0 until GRID_SIZE).map { arrayOfNulls<Tile?>(GRID_SIZE).toList() }

/**
 * Presenter class that contains the logic that powers the 2048 game.
 */
class GamePresenter(private val gameRepository: GameRepository) : Presenter<GameScreen.State> {

    class Factory(private val gameRepository: GameRepository) : Presenter.Factory {
        override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*> {
            return GamePresenter(gameRepository)
        }
    }

    @Composable
    override fun present(): GameScreen.State {
        val scope = rememberCoroutineScope()
        var grid: List<List<Tile?>> by rememberRetained { mutableStateOf(EMPTY_GRID) }
        var gridTileMovements by rememberRetained { mutableStateOf<List<GridTileMovement>>(listOf()) }
        var currentScore by rememberRetained { mutableIntStateOf(0) }
        var bestScore by rememberRetained { mutableIntStateOf(0) }
        var isGameOver by rememberRetained { mutableStateOf(false) }
        var moveCount by rememberRetained { mutableIntStateOf(0) } // TODO: unused.
        val userData by produceState<UserData?>(initialValue = null) { value = gameRepository.fetch() }

        fun save() {
            scope.launch {
                if (!isGameOver) {
                    gameRepository.update(grid, currentScore, bestScore)
                }
            }
        }

        fun startNewGame() {
            gridTileMovements = (0 until NUM_INITIAL_TILES).mapNotNull { createRandomAddedTile(EMPTY_GRID) }
            val addedGridTiles = gridTileMovements.map { it.toGridTile }
            grid = EMPTY_GRID.map { row, col, _ -> addedGridTiles.find { row == it.cell.row && col == it.cell.col }?.tile }
            currentScore = 0
            isGameOver = false
            moveCount = 0
            save()
        }

        fun move(direction: Direction) {
            var (updatedGrid, updatedGridTileMovements) = makeMove(grid, direction)

            if (!hasGridChanged(updatedGridTileMovements)) {
                // No tiles were moved.
                return
            }

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
            save()
        }

        LaunchedEffect(userData) {
            val data = userData
            if (grid == EMPTY_GRID && data != null) {
                bestScore = data.bestScore
                if (data.grid == null) {
                    startNewGame()
                } else {
                    // Restore a previously saved game.
                    grid = data.grid
                    gridTileMovements = data.grid
                        .flatMapIndexed { row, tiles ->
                            tiles.mapIndexed { col, tile ->
                                GridTileMovement.noop(GridTile(Cell(row, col), tile ?: return@mapIndexed null))
                            }
                        }
                        .filterNotNull()
                    currentScore = data.currentScore
                    isGameOver = checkIsGameOver(grid)
                }
            }
        }

        fun eventSink(event: GameScreen.Event) {
            when(event) {
                is GameScreen.Event.Move -> move(event.direction)
                GameScreen.Event.StartNewGame -> startNewGame()
            }
        }

        return GameScreen.State(
            gridTileMovements = gridTileMovements,
            currentScore = currentScore,
            bestScore = bestScore,
            isGameOver = isGameOver,
            eventSink = ::eventSink
        )
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
    require(numRotations in 0..3)
    return map { row, col, _ ->
        val (rotatedRow, rotatedCol) = getRotatedCellAt(row, col, numRotations)
        this[rotatedRow][rotatedCol]
    }
}

private fun getRotatedCellAt(row: Int, col: Int, numRotations: Int): Cell {
    require(numRotations in 0..3)
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
