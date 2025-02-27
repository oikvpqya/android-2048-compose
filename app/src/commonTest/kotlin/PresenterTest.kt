import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.serialization.encodeToSavedState
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.alexjlockwood.twentyfortyeight.domain.Cell
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.GridTileMovement
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.EventBus
import com.alexjlockwood.twentyfortyeight.ui.GamePresenter
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUiState
import com.alexjlockwood.twentyfortyeight.ui.rememberGamePresenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class PresenterTest {

    @Test
    fun produceEvent() = runTest {
        val eventBus = createEventBus()
        moleculeFlow(RecompositionMode.Immediate) {
            rememberGamePresenter(createRepository()).uiState(eventBus.eventFlow)
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            eventBus.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())
        }
    }

    @Test
    fun launchedEffect() = runTest {
        moleculeFlow(RecompositionMode.Immediate) {
            val eventBus = remember { createEventBus() }
            val uiState = rememberGamePresenter(createRepository()).uiState(eventBus.eventFlow)
            LaunchedEffect(Unit) {
                when (uiState) {
                    GameUiState.Nothing -> {
                        eventBus.produceEvent(GameUiEvent.Load)
                    }
                    GameUiState.Loading, is GameUiState.Success -> Unit
                }
            }
            uiState
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())
        }
    }

    @Test
    fun load() = runTest {
        val eventBus = createEventBus()
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        moleculeFlow(RecompositionMode.Immediate) {
            rememberGamePresenter(createRepository(userData)).uiState(eventBus.eventFlow)
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            eventBus.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())

            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            assertEquals(userData.currentScore, item.currentScore)
            assertEquals(userData.bestScore, item.bestScore)
        }
    }

    @Test
    fun startNewGame() = runTest {
        val eventBus = createEventBus()
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        moleculeFlow(RecompositionMode.Immediate)  {
            rememberGamePresenter(createRepository(userData)).uiState(eventBus.eventFlow)
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            eventBus.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())

            eventBus.produceEvent(GameUiEvent.StartNewGame)
            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            assertTrue { item.gridTileMovements.size == 2 }
        }
    }

    @Test
    fun move() = runTest {
        val eventBus = createEventBus()
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        moleculeFlow(RecompositionMode.Immediate)  {
            rememberGamePresenter(createRepository(userData)).uiState(eventBus.eventFlow)
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            eventBus.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())

            eventBus.produceEvent(GameUiEvent.Move(Direction.WEST))
            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            val tile = item.gridTileMovements.map {
                it.toGridTile
            }.filter {
                it.cell == Cell(0, 0)
            }.maxBy {
                it.tile.num
            }.tile
            assertEquals(32, tile.num)
        }
    }

    @Test
    fun undo() = runTest {
        val eventBus = createEventBus()
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        moleculeFlow(RecompositionMode.Immediate)  {
            rememberGamePresenter(createRepository(userData)).uiState(eventBus.eventFlow)
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            eventBus.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())
            eventBus.produceEvent(GameUiEvent.Move(Direction.WEST))
            assertIs<GameUiState.Success>(awaitItem())

            eventBus.produceEvent(GameUiEvent.Undo)
            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            val tile = item.gridTileMovements.map {
                it.toGridTile
            }.filter {
                it.cell == Cell(0, 0)
            }.maxBy {
                it.tile.num
            }.tile
            assertEquals(16, tile.num)
        }
    }

    @Test
    fun savedState() = runTest {
        val eventBus = createEventBus()
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = GamePresenter(createRepository(userData))
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.uiState(eventBus.eventFlow)
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            eventBus.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())

            val savedState = presenter.savedState()
            assertTrue { savedState.read { contains("grid") } }
            assertTrue { savedState.read { contains("gridTileMovements") } }
            assertTrue { savedState.read { contains("currentScore") } }
            assertTrue { savedState.read { contains("bestScore") } }
            assertTrue { savedState.read { contains("isGameOver") } }
            assertTrue { savedState.read { contains("moveCount") } }
            assertTrue { savedState.read { contains("canUndo") } }
            assertTrue { savedState.read { contains("stack") } }
            assertEquals(userData.currentScore, savedState.read { getInt("currentScore") })
            assertEquals(userData.bestScore, savedState.read { getInt("bestScore") })
        }
    }

    @Test
    fun restore() = runTest {
        val eventBus = createEventBus()
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val savedState = savedState {
            putSavedState("grid", encodeToSavedState(userData.grid!!))
            putSavedState("gridTileMovements", encodeToSavedState<List<GridTileMovement>>(emptyList()))
            putInt("currentScore", userData.currentScore)
            putInt("bestScore", userData.bestScore)
            putBoolean("isGameOver", false)
            putInt("moveCount", 0)
            putBoolean("canUndo", false)
            putSavedState("stack", encodeToSavedState(ArrayDeque<UserData>().toList()))
        }
        moleculeFlow(RecompositionMode.Immediate) {
            remember { GamePresenter(createRepository(userData), savedState = savedState) }.uiState(eventBus.eventFlow)
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            eventBus.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Success>(awaitItem())
        }
    }
}

private fun createEventBus(): EventBus<GameUiEvent> = EventBus()

private fun createRepository(
    userData: UserData = UserData.EMPTY_USER_DATA,
): GameRepository = object : GameRepository {
    override suspend fun fetch(): UserData {
        delay(100.milliseconds)
        return userData
    }

    override suspend fun update(grid: List<List<Tile?>>, currentScore: Int, bestScore: Int) {
        delay(100.milliseconds)
    }
}
