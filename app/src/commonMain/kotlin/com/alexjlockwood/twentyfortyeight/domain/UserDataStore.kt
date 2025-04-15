package com.alexjlockwood.twentyfortyeight.domain

import com.alexjlockwood.twentyfortyeight.ui.GRID_SIZE
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
    return MutableList(GRID_SIZE) { MutableList<Int?>(GRID_SIZE) { null } }.apply {
        this@toGrid.forEach { (tile, _, to) -> this@apply[to.row][to.col] = tile.num }
    }
}

private fun List<List<Int?>>.toNoopTileMovements(): List<GridTileMovement> {
    return flatMapIndexed { row, values ->
        values.mapIndexed { col, value -> if (value != null) GridTileMovement.noop(Tile(value), Cell(row, col)) else null }
    }.filterNotNull()
}
