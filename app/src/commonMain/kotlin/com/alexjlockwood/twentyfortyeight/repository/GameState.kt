package com.alexjlockwood.twentyfortyeight.repository

import com.alexjlockwood.twentyfortyeight.domain.Cell
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import kotlin.math.max
import kotlin.random.Random

const val GRID_SIZE = 4
private const val NUM_INITIAL_TILES = 2
private const val MAX_LIST_SIZE = 100

/**
 * Statement that contains the logic that powers the 2048 game.
 */
class GameState(
    private val gameRepository: GameRepository,
    maxStackSize: Int = MAX_LIST_SIZE,
) {

    var data = UserData()
    private val mutableStack = MutableLimitedList<UserData>(mutableListOf(), maxStackSize)
    val stack: List<UserData>
        get() = mutableStack

    suspend fun save(data: UserData) {
        if (!checkIsGameOver(data.movements)) { gameRepository.update(data) }
    }

    suspend fun startNewGame(): UserData {
        val updatedTileMovements = buildList<GridTileMovement> {
            repeat(NUM_INITIAL_TILES) { add(createRandomAddedTile(map { it.to })) }
        }
        val updatedData = UserData(updatedTileMovements, 0, data.bestScore)
        mutableStack.clear()
        data = updatedData
        save(updatedData)
        return updatedData
    }

    suspend fun move(
        direction: Direction,
    ): UserData? {
        val updatedData = moveTiles(data, direction) ?: return null
        // Push game data to stack.
        mutableStack.add(data)
        data = updatedData
        save(updatedData)
        return updatedData
    }

    suspend fun undo(): UserData? {
        if (mutableStack.isEmpty()) return null
        // Pop and restore game from stack.
        val updatedData = mutableStack.removeAt(mutableStack.lastIndex)
        data = updatedData
        save(updatedData)
        return updatedData
    }

    suspend fun load(useCache: Boolean): UserData? {
        return if (useCache) {
            if (data.movements.isNotEmpty()) data else null
        } else {
            val userData = gameRepository.fetch()
            if (userData.movements.isNotEmpty()) {
                // Restore a previously saved game.
                mutableStack.clear()
                data = userData
                userData
            } else {
                null
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

private class MutableLimitedList<T>(
    private val base: MutableList<T>,
    private val maxSize: Int = MAX_LIST_SIZE,
) : MutableList<T> by base {

    override fun add(element: T): Boolean {
        return if (base.add(element)) {
            while (size > maxSize) {
                removeAt(0)
            }
            true
        } else {
            false
        }
    }
}

fun UserData.checkIsGameOver(): Boolean {
    return checkIsGameOver(movements)
}
