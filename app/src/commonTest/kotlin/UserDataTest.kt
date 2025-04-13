import com.alexjlockwood.twentyfortyeight.domain.DEFAULT_JSON
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserDataTest {

    @Test
    fun decodeUserDataJson() = runTest {
        val data = DEFAULT_JSON.decodeFromString<UserData>(userDataJson)
        assertIs<UserData>(data)
        assertEquals(userData.resetTileId(), data.resetTileId())
        assertEquals(DEFAULT_JSON.encodeToString(userData), DEFAULT_JSON.encodeToString(data))
    }
}

private fun UserData.resetTileId(): UserData {
    return this.copy(grid = this.grid.map { row -> row.map { col -> col?.let { tile -> Tile(tile.num, 0) } } })
}

private val userData = UserData(
    grid= listOf(
        listOf(Tile(128), Tile(2), Tile(8), null),
        listOf(Tile(16), Tile(8), Tile(2), null),
        listOf(Tile(8), null, null, null),
        listOf(Tile(2), null, null, null),
    ),
    currentScore = 848,
    bestScore = 852,
)

private val userDataJson = """
{
    "grid": [
        [
            128,
            2,
            8,
            null
        ],
        [
            16,
            8,
            2,
            null
        ],
        [
            8,
            null,
            null,
            null
        ],
        [
            2,
            null,
            null,
            null
        ]
    ],
    "currentScore": 848,
    "bestScore": 852
}
""".trimIndent()
