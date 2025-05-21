package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.alexjlockwood.twentyfortyeight.domain.Cell
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.runtime.EventBusImpl
import com.alexjlockwood.twentyfortyeight.runtime.Presenter
import com.alexjlockwood.twentyfortyeight.runtime.PresenterImpl
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.random.Random

const val GRID_SIZE = 4
private const val NUM_INITIAL_TILES = 2
private const val MAX_STACK = 100

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
    ) : GameUiState {

        constructor(
            data: UserData,
            canUndo: Boolean,
        ) : this(
            gridTileMovements = data.movements,
            currentScore = data.currentScore,
            bestScore = data.bestScore,
            isGameOver = checkIsGameOver(data.movements),
            canUndo = canUndo,
        )
    }
}

@Serializable
data class GamePresenterState(
    var data: UserData = UserData(),
    val stack: MutableList<UserData> = mutableListOf(),
)

@Composable
fun rememberGamePresenter(
    gameRepository: GameRepository,
): Presenter<GameUiEvent, GameUiState> {
    val presenterState = rememberSaveable(
        saver = Saver(
            save = { encodeToSavedState(it) },
            restore = { decodeFromSavedState(it) },
        ),
    ) { GamePresenterState() }
    return remember(
        gameRepository, presenterState,
    ) { GamePresenter(PresenterImpl(EventBusImpl(), GameUiState.Nothing), gameRepository, presenterState) }
}

/**
 * Presenter that contains the logic that powers the 2048 game.
 */
class GamePresenter(
    base: Presenter<GameUiEvent, GameUiState>,
    private val gameRepository: GameRepository,
    private val presenterState: GamePresenterState,
) : Presenter<GameUiEvent, GameUiState> by base {

    private suspend fun save(data: UserData) {
        if (!checkIsGameOver(data.movements)) { gameRepository.update(data) }
    }

    private suspend fun startNewGame() {
        val updatedTileMovements = buildList<GridTileMovement> {
            repeat(NUM_INITIAL_TILES) { add(createRandomAddedTile(map { it.to })) }
        }
        val updatedData = UserData(updatedTileMovements, 0, presenterState.data.bestScore)
        presenterState.stack.clear()
        presenterState.data = updatedData
        save(updatedData)
        produceUiState(GameUiState.Success(updatedData, false))
    }

    private suspend fun move(
        direction: Direction,
    ) {
        val updatedData = moveTiles(presenterState.data, direction) ?: return
        // Push game data to stack.
        presenterState.stack.add(presenterState.data)
        while (presenterState.stack.size > MAX_STACK) {
            presenterState.stack.removeAt(0)
        }
        presenterState.data = updatedData
        save(updatedData)
        produceUiState(GameUiState.Success(updatedData, presenterState.stack.isNotEmpty()))
    }

    private suspend fun undo() {
        if (presenterState.stack.isEmpty()) return
        // Pop and restore game from stack.
        val updatedData = presenterState.stack.removeAt(presenterState.stack.lastIndex)
        presenterState.data = updatedData
        save(updatedData)
        produceUiState(GameUiState.Success(updatedData, presenterState.stack.isNotEmpty()))
    }

    private suspend fun load() {
        if (presenterState.data.movements.isNotEmpty()) {
            produceUiState(GameUiState.Success(presenterState.data, presenterState.stack.isNotEmpty()))
            return
        }
        produceUiState(GameUiState.Loading)
        val userData = gameRepository.fetch()
        if (userData.movements.isEmpty()) {
            startNewGame()
            return
        }
        // Restore a previously saved game.
        presenterState.stack.clear()
        presenterState.data = userData
        produceUiState(GameUiState.Success(userData, false))
    }

    override suspend fun handleEvent(event: GameUiEvent) {
        when (event) {
            GameUiEvent.Load -> {
                load()
            }
            is GameUiEvent.Move -> {
                move(event.direction)
            }
            GameUiEvent.StartNewGame -> {
                startNewGame()
            }
            GameUiEvent.Undo -> {
                undo()
            }
        }
    }
}

private fun moveTiles(
    data: UserData,
    direction: Direction,
): UserData? {
    val movedTileMovements = makeMove(data.movements, direction)

    if (!hasGridChanged(movedTileMovements)) {
        // No tiles were moved.
        return null
    }

    // Increment the score.
    val scoreIncrement = movedTileMovements.filter { it.from == null }.sumOf { it.tile.num }
    val score = data.currentScore + scoreIncrement

    // Attempt to add a new tile to the grid.
    val addedTileMovements = movedTileMovements.toMutableList().apply {
        add(createRandomAddedTile(movedTileMovements.map { it.to }))
    }.sortedWith { a, _ -> if (a.from == null) 1 else -1 }

    return UserData(addedTileMovements, score, max(data.bestScore, score))
}

private fun createRandomAddedTile(cells: List<Cell>): GridTileMovement {
    val emptyCells = buildList<Cell> {
        repeat(GRID_SIZE) { rowIndex ->
            repeat(GRID_SIZE) { colIndex ->
                val cell = Cell(rowIndex, colIndex)
                if (!cells.contains(cell)) add(cell)
            }
        }
    }
    return GridTileMovement.add(Tile(if (Random.nextFloat() < 0.9f) 2 else 4), emptyCells[emptyCells.indices.random()])
}

private fun makeMove(movements: List<GridTileMovement>, direction: Direction): List<GridTileMovement> {
    return buildList {
        val tiles = movements.groupBy { it.to }.mapValues { (_, value) -> value.maxBy { it.tile.id }.tile }
        repeat(GRID_SIZE) { currentRowIndex ->
            // Rotate tiles so that we can process it as if the user has swiped their
            // finger from right to left
            val mutableRowTiles = MutableList(GRID_SIZE) { tiles[getRotatedCellAt(direction, currentRowIndex, it)] }
            var lastSeenTileIndex: Int? = null
            var lastSeenEmptyIndex: Int? = null
            repeat(GRID_SIZE) { currentColIndex ->
                val currentTile = mutableRowTiles[currentColIndex]
                val currentCell = getRotatedCellAt(direction, currentRowIndex, currentColIndex)
                when {
                    currentTile == null -> {
                        // We are looking at an empty cell in the grid.
                        if (lastSeenEmptyIndex == null) {
                            // Keep track of the first empty index we find.
                            lastSeenEmptyIndex = currentColIndex
                        }
                    }

                    // Otherwise, we have encountered a tile that could either be shifted,
                    // merged, or not moved at all.
                    lastSeenTileIndex == null -> {
                        // This is the first tile in the list that we've found.
                        if (lastSeenEmptyIndex == null) {
                            // Keep the tile at its same location.
                            add(GridTileMovement.noop(currentTile, currentCell))
                            lastSeenTileIndex = currentColIndex
                        } else {
                            // Shift the tile to the location of the furthest empty cell in the list.
                            val targetCell = getRotatedCellAt(direction, currentRowIndex, lastSeenEmptyIndex)
                            add(GridTileMovement.shift(currentTile, currentCell, targetCell))

                            mutableRowTiles[lastSeenEmptyIndex] = currentTile
                            mutableRowTiles[currentColIndex] = null
                            lastSeenTileIndex = lastSeenEmptyIndex
                            lastSeenEmptyIndex++
                        }
                    }

                    // There is a previous tile in the list that we need to process.
                    mutableRowTiles[lastSeenTileIndex]!!.num == currentTile.num -> {
                        // Shift the tile to the location where it will be merged.
                        val targetCell = getRotatedCellAt(direction, currentRowIndex, lastSeenTileIndex)
                        add(GridTileMovement.shift(currentTile, currentCell, targetCell))

                        // Merge the current tile with the previous tile.
                        val addedTile = currentTile * 2
                        add(GridTileMovement.add(addedTile, targetCell))

                        mutableRowTiles[lastSeenTileIndex] = addedTile
                        mutableRowTiles[currentColIndex] = null
                        lastSeenTileIndex = null
                        if (lastSeenEmptyIndex == null) {
                            lastSeenEmptyIndex = currentColIndex
                        }
                    }

                    lastSeenEmptyIndex == null -> {
                        // Keep the tile at its same location.
                        add(GridTileMovement.noop(currentTile, currentCell))
                        lastSeenTileIndex++
                    }

                    else -> {
                        // Shift the current tile towards the previous tile.
                        val targetCell = getRotatedCellAt(direction, currentRowIndex, lastSeenEmptyIndex)
                        add(GridTileMovement.shift(currentTile, currentCell, targetCell))

                        mutableRowTiles[lastSeenEmptyIndex] = currentTile
                        mutableRowTiles[currentColIndex] = null
                        lastSeenTileIndex++
                        lastSeenEmptyIndex++
                    }
                }
            }
        }
    }
}

private fun getRotatedCellAt(direction: Direction, row: Int, col: Int): Cell {
    return when (direction) {
        Direction.WEST -> Cell(row, col)
        Direction.SOUTH -> Cell(GRID_SIZE - 1 - col, row)
        Direction.EAST -> Cell(GRID_SIZE - 1 - row, GRID_SIZE - 1 - col)
        Direction.NORTH -> Cell(col, GRID_SIZE - 1 - row)
    }
}

private fun checkIsGameOver(movements: List<GridTileMovement>): Boolean {
    // The game is over if no tiles can be moved in any of the 4 directions.
    return Direction.entries.none { hasGridChanged(makeMove(movements, it)) }
}

private fun hasGridChanged(movements: List<GridTileMovement>): Boolean {
    // The grid has changed if any of the tiles have moved to a different location.
    return movements.any { (_, from, to) -> from == null || from != to }
}
