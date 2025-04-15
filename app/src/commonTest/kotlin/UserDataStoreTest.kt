import com.alexjlockwood.twentyfortyeight.domain.DEFAULT_JSON
import com.alexjlockwood.twentyfortyeight.domain.UserDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserDataStoreTest {

    @Test
    fun decodeUserDataStoreJson() = runTest {
        val data = DEFAULT_JSON.decodeFromString<UserDataStore>(storeJson)
        assertIs<UserDataStore>(data)
        assertEquals(store.grid, data.grid)
        assertEquals(store.currentScore, data.currentScore)
        assertEquals(store.bestScore, data.bestScore)
        assertEquals(DEFAULT_JSON.encodeToString(store), DEFAULT_JSON.encodeToString(data))
    }
}

private val store = UserDataStore(
    grid = listOf(
        listOf(128, 2, 8, null),
        listOf(16, 8, 2, null),
        listOf(8, null, null, null),
        listOf(2, null, null, null),
    ),
    currentScore = 848,
    bestScore = 852,
)

private val storeJson = """
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
