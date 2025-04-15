package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.alexjlockwood.twentyfortyeight.domain.Cell
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
): GamePresenter {
    return rememberSaveable(
        gameRepository,
        saver = Saver(
            save = { it.encodePresenterStateToSavedState() },
            restore = { GamePresenter(gameRepository = gameRepository, presenterState = decodeFromSavedState(it)) },
        ),
    ) { GamePresenter(gameRepository = gameRepository) }
}

/**
 * Presenter that contains the logic that powers the 2048 game.
 */
class GamePresenter(
    private val gameRepository: GameRepository,
    private val presenterState: GamePresenterState = GamePresenterState(),
) : EventBus<GameUiEvent> by EventBusImpl() {

    private val mutableUiStateFlow = MutableStateFlow<GameUiState>(GameUiState.Nothing)
    val uiStateFlow = mutableUiStateFlow.asStateFlow()

    private suspend fun save(data: UserData) {
        if (!checkIsGameOver(data.movements)) { gameRepository.update(data) }
    }

    private suspend fun startNewGame() {
        val updatedTileMovements = initializeTiles()
        val updatedData = UserData(updatedTileMovements, 0, presenterState.data.bestScore)
        presenterState.stack.clear()
        presenterState.data = updatedData
        save(updatedData)
        mutableUiStateFlow.value = GameUiState.Success(updatedData, false)
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
        mutableUiStateFlow.value = GameUiState.Success(updatedData, presenterState.stack.isNotEmpty())
    }

    private suspend fun undo() {
        if (presenterState.stack.isEmpty()) return
        // Pop and restore game from stack.
        val updatedData = presenterState.stack.removeAt(presenterState.stack.lastIndex)
        presenterState.data = updatedData
        save(updatedData)
        mutableUiStateFlow.value = GameUiState.Success(updatedData, presenterState.stack.isNotEmpty())
    }

    private suspend fun load() {
        if (presenterState.data.movements.isNotEmpty()) {
            mutableUiStateFlow.value = GameUiState.Success(presenterState.data, presenterState.stack.isNotEmpty())
            return
        }
        mutableUiStateFlow.value = GameUiState.Loading
        val userData = gameRepository.fetch()
        if (userData.movements.isEmpty()) {
            startNewGame()
            return
        }
        // Restore a previously saved game.
        presenterState.stack.clear()
        presenterState.data = userData
        mutableUiStateFlow.value = GameUiState.Success(userData, false)
    }

    suspend fun handleEvent(event: GameUiEvent) {
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

    @Composable
    fun UiStateProvider(
        content: @Composable (GameUiState) -> Unit,
    ) {
        LaunchedEffect(Unit) {
            eventFlow.collect { event ->
                handleEvent(event)
            }
        }
        val uiState by uiStateFlow.collectAsState()
        content(uiState)
    }

    fun encodePresenterStateToSavedState(): SavedState {
        return encodeToSavedState(presenterState)
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

private fun initializeTiles(): List<GridTileMovement> {
    return List(NUM_INITIAL_TILES) { createRandomAddedTile(emptyList()) }.distinctBy { it.to }
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
    val numRotations = when (direction) {
        Direction.WEST -> 0
        Direction.SOUTH -> 1
        Direction.EAST -> 2
        Direction.NORTH -> 3
    }
    val cellTileMap = movements.associate { movement -> movement.to to movement.tile }

    // Rotate the grid so that we can process it as if the user has swiped their
    // finger from right to left.
    val updatedGrid = List(GRID_SIZE) { rowIndex ->
        List(GRID_SIZE) { colIndex ->
            val cell = getRotatedCellAt(rowIndex, colIndex, numRotations)
            if (cellTileMap.containsKey(cell)) cellTileMap[cell] else null
        }
    }

    val movedTileMovements = mutableListOf<GridTileMovement>()
    repeat(GRID_SIZE) { currentRowIndex ->
        val mutableRowTiles = updatedGrid[currentRowIndex].toMutableList()
        var lastSeenTileIndex: Int? = null
        var lastSeenEmptyIndex: Int? = null
        repeat(GRID_SIZE) { currentColIndex ->
            val currentTile = mutableRowTiles[currentColIndex]
            if (currentTile == null) {
                // We are looking at an empty cell in the grid.
                if (lastSeenEmptyIndex == null) {
                    // Keep track of the first empty index we find.
                    lastSeenEmptyIndex = currentColIndex
                }
                return@repeat
            }

            // Otherwise, we have encountered a tile that could either be shifted,
            // merged, or not moved at all.
            val currentCell = getRotatedCellAt(currentRowIndex, currentColIndex, numRotations)

            if (lastSeenTileIndex == null) {
                // This is the first tile in the list that we've found.
                if (lastSeenEmptyIndex == null) {
                    // Keep the tile at its same location.
                    movedTileMovements.add(GridTileMovement.noop(currentTile, currentCell))
                    lastSeenTileIndex = currentColIndex
                } else {
                    // Shift the tile to the location of the furthest empty cell in the list.
                    val targetCell = getRotatedCellAt(currentRowIndex, lastSeenEmptyIndex, numRotations)
                    movedTileMovements.add(GridTileMovement.shift(currentTile, currentCell, targetCell))

                    mutableRowTiles[lastSeenEmptyIndex] = currentTile
                    mutableRowTiles[currentColIndex] = null
                    lastSeenTileIndex = lastSeenEmptyIndex
                    lastSeenEmptyIndex++
                }
            } else {
                // There is a previous tile in the list that we need to process.
                if (mutableRowTiles[lastSeenTileIndex]!!.num == currentTile.num) {
                    // Shift the tile to the location where it will be merged.
                    val targetCell = getRotatedCellAt(currentRowIndex, lastSeenTileIndex, numRotations)
                    movedTileMovements.add(GridTileMovement.shift(currentTile, currentCell, targetCell))

                    // Merge the current tile with the previous tile.
                    val addedTile = currentTile * 2
                    movedTileMovements.add(GridTileMovement.add(addedTile, targetCell))

                    mutableRowTiles[lastSeenTileIndex] = addedTile
                    mutableRowTiles[currentColIndex] = null
                    lastSeenTileIndex = null
                    if (lastSeenEmptyIndex == null) {
                        lastSeenEmptyIndex = currentColIndex
                    }
                } else {
                    if (lastSeenEmptyIndex == null) {
                        // Keep the tile at its same location.
                        movedTileMovements.add(GridTileMovement.noop(currentTile, currentCell))
                    } else {
                        // Shift the current tile towards the previous tile.
                        val targetCell = getRotatedCellAt(currentRowIndex, lastSeenEmptyIndex, numRotations)
                        movedTileMovements.add(GridTileMovement.shift(currentTile, currentCell, targetCell))

                        mutableRowTiles[lastSeenEmptyIndex] = currentTile
                        mutableRowTiles[currentColIndex] = null
                        lastSeenEmptyIndex++
                    }
                    lastSeenTileIndex++
                }
            }
        }
    }

    return movedTileMovements
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

private fun checkIsGameOver(movements: List<GridTileMovement>): Boolean {
    // The game is over if no tiles can be moved in any of the 4 directions.
    return Direction.entries.none { hasGridChanged(makeMove(movements, it)) }
}

private fun hasGridChanged(movements: List<GridTileMovement>): Boolean {
    // The grid has changed if any of the tiles have moved to a different location.
    return movements.any { (_, from, to) -> from == null || from != to }
}
