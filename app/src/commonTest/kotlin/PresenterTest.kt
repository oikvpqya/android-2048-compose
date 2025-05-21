import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.domain.UserDataStore
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.runtime.EventBusImpl
import com.alexjlockwood.twentyfortyeight.runtime.Presenter
import com.alexjlockwood.twentyfortyeight.runtime.PresenterImpl
import com.alexjlockwood.twentyfortyeight.runtime.collectAsUiState
import com.alexjlockwood.twentyfortyeight.ui.GamePresenter
import com.alexjlockwood.twentyfortyeight.ui.GamePresenterState
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUiState
import com.alexjlockwood.twentyfortyeight.ui.rememberGamePresenter
import kotlinx.coroutines.delay
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
        val presenter = createPresenter(createRepository(), GamePresenterState())
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.collectAsUiState().value
        }.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.produceEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())
        }
    }

    @Test
    fun launchedEffect() = runTest {
        val repository = createRepository()
        moleculeFlow(RecompositionMode.Immediate) {
            val presenter = rememberGamePresenter(repository)
            val uiState by presenter.collectAsUiState()
            LaunchedEffect(uiState) {
                when (uiState) {
                    GameUiState.Nothing -> {
                        presenter.produceEvent(GameUiEvent.Load)
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
        val store = UserDataStore(
            grid = List(4) { List(4) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(store), GamePresenterState())
        presenter.uiStateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())

            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            assertEquals(store.currentScore, item.currentScore)
            assertEquals(store.bestScore, item.bestScore)
        }
    }

    @Test
    fun startNewGame() = runTest {
        val store = UserDataStore(
            grid = List(4) { List(4) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(store), GamePresenterState())
        presenter.uiStateFlow.test {
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
        }
    }

    @Test
    fun move() = runTest {
        val store = UserDataStore(
            grid = List(4) { List(4) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(store), GamePresenterState())
        presenter.uiStateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())

            presenter.handleEvent(GameUiEvent.Move(Direction.WEST))
            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            assertEquals(32, item.gridTileMovements.maxBy { it.tile.num }.tile.num)
        }
    }

    @Test
    fun undo() = runTest {
        val store = UserDataStore(
            grid = List(4) { List(4) { 16 } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(store), GamePresenterState())
        presenter.uiStateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            val item1 = awaitItem()
            assertIs<GameUiState.Success>(item1)
            assertFalse(item1.canUndo)

            presenter.handleEvent(GameUiEvent.Move(Direction.WEST))
            val item2 = awaitItem()
            assertIs<GameUiState.Success>(item2)
            assertTrue(item2.canUndo)

            presenter.handleEvent(GameUiEvent.Undo)
            val item3 = awaitItem()
            assertIs<GameUiState.Success>(item3)
            assertTrue(item3.gridTileMovements.all { it.tile.num == 16 })
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
        val presenter = createPresenter(createRepository(store), GamePresenterState())
        presenter.uiStateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            val item1 = awaitItem()
            assertIs<GameUiState.Success>(item1)
            assertFalse(item1.isGameOver)
            assertFalse(item1.canUndo)

            presenter.handleEvent(GameUiEvent.Move(Direction.NORTH))
            val item2 = awaitItem()
            assertIs<GameUiState.Success>(item2)
            assertTrue(item2.isGameOver)
            assertTrue(item2.canUndo)

            presenter.handleEvent(GameUiEvent.Undo)
            assertIs<GameUiState.Success>(awaitItem())
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

private fun createPresenter(
    repository: GameRepository,
    presenterState: GamePresenterState,
): Presenter<GameUiEvent, GameUiState> = GamePresenter(PresenterImpl(EventBusImpl(), GameUiState.Nothing), repository, presenterState)
