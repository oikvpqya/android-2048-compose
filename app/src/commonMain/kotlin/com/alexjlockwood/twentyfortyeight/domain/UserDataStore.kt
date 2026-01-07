package com.alexjlockwood.twentyfortyeight.domain

import com.alexjlockwood.twentyfortyeight.repository.GameStrategy
import kotlinx.serialization.Serializable

@Serializable
data class UserDataStore(
    val grid: List<List<Int?>> = emptyList(),
    val currentScore: Int = 0,
    val bestScore: Int = 0,
) {

    constructor(data: UserData) : this(
        grid = data.movements.toGrid(),
        currentScore = data.currentScore,
        bestScore = data.bestScore,
    )

    fun toUserData(): UserData {
        return UserData(grid.toNoopTileMovements(), currentScore, bestScore)
    }
}

private fun List<GridTileMovement>.toGrid(): List<List<Int?>> {
    return toGrid(GameStrategy.DEFAULT.gridSize)
}

private fun List<GridTileMovement>.toGrid(gridSize: Int): List<List<Int?>> {
    val values = groupBy { it.to }.mapValues { (_, value) -> value.maxBy { it.tile.id }.tile.num }
    return List(gridSize) { row -> List(gridSize) { col -> values[Cell(row, col)] } }
}

private fun List<List<Int?>>.toNoopTileMovements(): List<GridTileMovement> {
    return flatMapIndexed { row, values ->
        require(size == values.size)
        values.mapIndexed { col, value -> if (value != null) GridTileMovement.noop(Tile(value), Cell(row, col)) else null }
    }.filterNotNull()
}
