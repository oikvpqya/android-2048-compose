import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.alexjlockwood.twentyfortyeight.domain.Cell
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.GamePresenter
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class PresenterTest {

    @Test
    fun produceEvent() = runTest {
        val eventBus = createEventBus()
        moleculeFlow(RecompositionMode.Immediate) {
            remember { createPresenter() }.uiState(eventBus.eventFlow)
        }.test {
            assertTrue { awaitItem() is GameUiState.Nothing }
            eventBus.produceEvent(GameUiEvent.Load)
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }
        }
    }

    @Test
    fun launchedEffect() = runTest {
        moleculeFlow(RecompositionMode.Immediate) {
            val eventBus = remember { createEventBus() }
            val uiState = remember { createPresenter() }.uiState(eventBus.eventFlow)
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
            assertTrue { awaitItem() is GameUiState.Nothing }
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }
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
            remember { createPresenter(createRepository(userData)) }.uiState(eventBus.eventFlow)
        }.test {
            assertTrue { awaitItem() is GameUiState.Nothing }
            eventBus.produceEvent(GameUiEvent.Load)
            assertTrue { awaitItem() is GameUiState.Loading }

            val item = awaitItem()
            assertTrue { item is GameUiState.Success }
            item as GameUiState.Success
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
            remember { createPresenter(createRepository(userData)) }.uiState(eventBus.eventFlow)
        }.test {
            assertTrue { awaitItem() is GameUiState.Nothing }
            eventBus.produceEvent(GameUiEvent.Load)
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }

            eventBus.produceEvent(GameUiEvent.StartNewGame)
            val item = awaitItem()
            assertTrue { item is GameUiState.Success }
            item as GameUiState.Success
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
            remember { createPresenter(createRepository(userData)) }.uiState(eventBus.eventFlow)
        }.test {
            assertTrue { awaitItem() is GameUiState.Nothing }
            eventBus.produceEvent(GameUiEvent.Load)
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }

            eventBus.produceEvent(GameUiEvent.Move(Direction.WEST))
            val item = awaitItem()
            assertTrue { item is GameUiState.Success }
            item as GameUiState.Success
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
            remember { createPresenter(createRepository(userData)) }.uiState(eventBus.eventFlow)
        }.test {
            assertTrue { awaitItem() is GameUiState.Nothing }
            eventBus.produceEvent(GameUiEvent.Load)
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }
            eventBus.produceEvent(GameUiEvent.Move(Direction.WEST))
            assertTrue { awaitItem() is GameUiState.Success }

            eventBus.produceEvent(GameUiEvent.Undo)
            val item = awaitItem()
            assertTrue { item is GameUiState.Success }
            item as GameUiState.Success
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
}

private class EventBus<EVENT> {

    val eventFlow = MutableSharedFlow<EVENT>(extraBufferCapacity = 20)
    fun produceEvent(event: EVENT) = eventFlow.tryEmit(event)
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

private fun createPresenter(
    repository: GameRepository = createRepository(),
): GamePresenter = GamePresenter(repository)
