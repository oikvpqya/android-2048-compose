import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.UserDataStore
import com.alexjlockwood.twentyfortyeight.repository.GameStrategy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StrategyTest {

    @Test
    fun startNewGame() = runTest {
        repeat(50) {
            val gridSize = (2..16).random()
            val strategy = GameStrategy(gridSize)
            val data = strategy.startNewGame(0)
            data.movements.size
            assertTrue { data.movements.size == 2 }
        }
    }

    @Test
    fun move() = runTest {
        val store = UserDataStore(
            grid = List(5) { List(5) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val (movements, currentScore, bestScore) = store.toUserData()
        val strategy = GameStrategy(5)
        val data = strategy.move(Direction.WEST, movements, currentScore, bestScore)
        assertNotNull(data)
        assertEquals(32, data.movements.maxBy { it.tile.num }.tile.num)
    }

    @Test
    fun gameOver() = runTest {
        val grid: List<List<Int>> = let { _ ->
            var num = 2
            val grid = MutableList(4) {
                MutableList(4) {
                    val ret = num
                    num *= 2
                    ret
                }
            }
            grid[0][0] = 32
            grid
        }
        val store = UserDataStore(
            grid = grid,
            currentScore = 16,
            bestScore = 32,
        )
        val (movements, currentScore, bestScore) = store.toUserData()
        val strategy = GameStrategy(4)
        assertFalse(strategy.checkIsGameOver(movements))

        val data = strategy.move(Direction.NORTH, movements, currentScore, bestScore)
        assertNotNull(data)
        assertTrue(strategy.checkIsGameOver(data.movements))
    }
}
