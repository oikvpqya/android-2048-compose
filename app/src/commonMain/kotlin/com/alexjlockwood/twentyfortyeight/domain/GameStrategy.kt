package com.alexjlockwood.twentyfortyeight.domain

import kotlin.math.max
import kotlin.random.Random

/**
 * Strategy that contains the logic that powers the 2048 game.
 */
class GameStrategy(
    val gridSize: Int,
    val initialTilesCount: Int = 2,
) {

    fun startNewGame(bestScore: Int): UserData {
        val initialMovements = buildList {
            repeat(initialTilesCount) {
                val cells = map { movement: GridTileMovement -> movement.to }
                add(createRandomAddedMovement(cells, gridSize))
            }
        }
        return UserData(initialMovements, 0, bestScore)
    }

    fun move(direction: Direction, movements: List<GridTileMovement>, currentScore: Int, bestScore: Int): UserData? {
        val movedMovements = makeMove(movements, direction, gridSize)

        if (!hasGridChanged(movedMovements)) {
            // No tiles were moved.
            return null
        }

        // Increment the score.
        val scoreIncrement = movedMovements.filter { it.from == null }.sumOf { it.tile.num }
        val updatedScore = currentScore + scoreIncrement

        // Attempt to add a new tile to the grid.
        val randomAddedMovements = movedMovements
            .toMutableList()
            .apply {
                add(createRandomAddedMovement(map { it.to }, gridSize))
            }
        return UserData(randomAddedMovements, updatedScore, max(bestScore, updatedScore))
    }

    fun checkIsGameOver(movements: List<GridTileMovement>): Boolean {
        return checkIsGameOver(movements, gridSize)
    }

    companion object {
        val DEFAULT: GameStrategy = GameStrategy(4)
    }
}

private fun createRandomAddedMovement(cells: List<Cell>, gridSize: Int): GridTileMovement {
    val emptyCells = buildList {
        repeat(gridSize) { rowIndex ->
            repeat(gridSize) { colIndex ->
                val cell = Cell(rowIndex, colIndex)
                if (!cells.contains(cell)) {
                    add(cell)
                }
            }
        }
    }
    return GridTileMovement.add(Tile(if (Random.nextFloat() < 0.9f) 2 else 4), emptyCells[emptyCells.indices.random()])
}

private fun makeMove(movements: List<GridTileMovement>, direction: Direction, gridSize: Int): List<GridTileMovement> {
    return buildList {
        val tiles = movements.groupBy { it.to }.mapValues { (_, value) -> value.maxBy { it.tile.id }.tile }
        repeat(gridSize) { currentRowIndex ->
            // Rotate tiles so that we can process it as if the user has swiped their
            // finger from right to left
            val mutableRowTiles = MutableList(gridSize) { tiles[getRotatedCellAt(direction, gridSize, currentRowIndex, it)] }
            var lastSeenTileIndex: Int? = null
            var lastSeenEmptyIndex: Int? = null
            repeat(gridSize) { currentColIndex ->
                val currentTile = mutableRowTiles[currentColIndex]
                val currentCell = getRotatedCellAt(direction, gridSize, currentRowIndex, currentColIndex)
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
                            val targetCell = getRotatedCellAt(direction, gridSize, currentRowIndex, lastSeenEmptyIndex)
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
                        val targetCell = getRotatedCellAt(direction, gridSize, currentRowIndex, lastSeenTileIndex)
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
                        val targetCell = getRotatedCellAt(direction, gridSize, currentRowIndex, lastSeenEmptyIndex)
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

private fun getRotatedCellAt(direction: Direction, gridSize: Int, row: Int, col: Int): Cell {
    return when (direction) {
        Direction.WEST -> Cell(row, col)
        Direction.SOUTH -> Cell(gridSize - 1 - col, row)
        Direction.EAST -> Cell(gridSize - 1 - row, gridSize - 1 - col)
        Direction.NORTH -> Cell(col, gridSize - 1 - row)
    }
}

private fun checkIsGameOver(movements: List<GridTileMovement>, gridSize: Int): Boolean {
    // The game is over if no tiles can be moved in any of the 4 directions.
    return Direction.entries.none { hasGridChanged(makeMove(movements, it, gridSize)) }
}

private fun hasGridChanged(movements: List<GridTileMovement>): Boolean {
    // The grid has changed if any of the tiles have moved to a different location.
    return movements.any { (_, from, to) -> from == null || from != to }
}
