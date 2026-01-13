import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.MutableLimitedList
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.domain.UserDataStore
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.GameState
import com.alexjlockwood.twentyfortyeight.runtime.EventBus
import com.alexjlockwood.twentyfortyeight.runtime.Presenter
import com.alexjlockwood.twentyfortyeight.runtime.buildEventBus
import com.alexjlockwood.twentyfortyeight.runtime.collectAsState
import com.alexjlockwood.twentyfortyeight.ui.GamePresenter
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUiState
import com.alexjlockwood.twentyfortyeight.ui.rememberGamePresenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class PresenterTest {

    @Test
    fun produceEvent() = runTest {
        val presenter = createPresenter(createRepository())
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.collectAsState().value
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun rememberGamePresenter() = runTest {
        val repository = createRepository()
        val state = GameState()
        val eventBus = buildEventBus<GameUiEvent>()
        moleculeFlow(RecompositionMode.Immediate) {
            rememberGamePresenter(state, eventBus, repository).collectAsState().value
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())
            eventBus.produceEvent(GameUiEvent.StartNewGame)
            assertIs<GameUiState.Success>(awaitItem())
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun load() = runTest {
        val store = UserDataStore(
            grid = List(4) { List(4) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(store))
        presenter.stateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())

            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            assertEquals(store.grid.size, item.gridSize)
            assertEquals(store.currentScore, item.currentScore)
            assertEquals(store.bestScore, item.bestScore)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun startNewGame() = runTest {
        val store = UserDataStore(
            grid = List(4) { List(4) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(store))
        presenter.stateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())

            repeat(50) {
                presenter.handleEvent(GameUiEvent.StartNewGame)
                val item = awaitItem()
                assertIs<GameUiState.Success>(item)
                assertTrue { item.gridTileMovements.size == 2 }
            }
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun move() = runTest {
        val store = UserDataStore(
            grid = List(4) { List(4) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(store))
        presenter.stateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())

            presenter.handleEvent(GameUiEvent.Move(Direction.WEST))
            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            assertEquals(32, item.gridTileMovements.maxBy { it.tile.num }.tile.num)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun undo() = runTest {
        val store = UserDataStore(
            grid = List(4) { List(4) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val mutableStack = MutableLimitedList<UserData>(mutableListOf(), 2)
        val presenter = createPresenter(createRepository(store), mutableStack = mutableStack)
        presenter.stateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            val item1 = awaitItem()
            assertIs<GameUiState.Success>(item1)
            assertFalse(item1.isUndoable)
            assertEquals(mutableStack.size, 1)

            presenter.handleEvent(GameUiEvent.Move(Direction.WEST))
            val item2 = awaitItem()
            assertIs<GameUiState.Success>(item2)
            assertTrue(item2.isUndoable)
            assertEquals(mutableStack.size, 2)
            presenter.handleEvent(GameUiEvent.Undo)
            val item3 = awaitItem()
            assertIs<GameUiState.Success>(item3)
            assertTrue(item3.gridTileMovements.all { it.tile.num == 16 })
            assertFalse(item3.isUndoable)
            assertEquals(mutableStack.size, 1)

            presenter.handleEvent(GameUiEvent.Move(Direction.WEST))
            awaitItem()
            assertEquals(mutableStack.size, 2)
            presenter.handleEvent(GameUiEvent.Move(Direction.WEST))
            val item4 = awaitItem()
            assertIs<GameUiState.Success>(item4)
            assertTrue(item4.isUndoable)
            assertEquals(mutableStack.size, 2)
            presenter.handleEvent(GameUiEvent.Undo)
            val item5 = awaitItem()
            assertIs<GameUiState.Success>(item5)
            assertFalse(item5.isUndoable)
            assertEquals(mutableStack.size, 1)
            ensureAllEventsConsumed()
        }
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
        val presenter = createPresenter(createRepository(store))
        presenter.stateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            val item1 = awaitItem()
            assertIs<GameUiState.Success>(item1)
            assertFalse(item1.isGameOver)
            assertFalse(item1.isUndoable)

            presenter.handleEvent(GameUiEvent.Move(Direction.NORTH))
            val item2 = awaitItem()
            assertIs<GameUiState.Success>(item2)
            assertTrue(item2.isGameOver)
            assertTrue(item2.isUndoable)

            presenter.handleEvent(GameUiEvent.Undo)
            assertIs<GameUiState.Success>(awaitItem())
            ensureAllEventsConsumed()
        }
    }
}

private fun createRepository(
    store: UserDataStore = UserDataStore(),
): GameRepository = object : GameRepository {
    override suspend fun fetch(): UserData {
        delay(100.milliseconds)
        return store.toUserData()
    }

    override suspend fun update(data: UserData) {
        delay(100.milliseconds)
    }
}

private fun TestScope.createPresenter(
    repository: GameRepository,
    eventBus: EventBus<GameUiEvent> = buildEventBus(),
    mutableStack: MutableList<UserData> = mutableListOf(),
): Presenter<GameUiEvent, GameUiState> = GamePresenter(repository, mutableStack, this, eventBus)
