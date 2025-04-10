import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        val presenter = GamePresenter(createRepository())
        moleculeFlow(RecompositionMode.Immediate) {
            LaunchedEffect(Unit) {
                presenter.eventFlow.collect { event ->
                    presenter.handleEvent(event)
                }
            }
            val uiState by presenter.uiStateFlow.collectAsState()
            uiState
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
            LaunchedEffect(presenter) {
                presenter.eventFlow.collect { event ->
                    presenter.handleEvent(event)
                }
            }
            val uiState by presenter.uiStateFlow.collectAsState()
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
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = GamePresenter(createRepository(userData))
        presenter.uiStateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())

            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            assertEquals(userData.currentScore, item.currentScore)
            assertEquals(userData.bestScore, item.bestScore)
        }
    }

    @Test
    fun startNewGame() = runTest {
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = GamePresenter(createRepository(userData))
        presenter.uiStateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())

            presenter.handleEvent(GameUiEvent.StartNewGame)
            val item = awaitItem()
            assertIs<GameUiState.Success>(item)
            assertTrue { item.gridTileMovements.size == 2 }
        }
    }

    @Test
    fun move() = runTest {
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = GamePresenter(createRepository(userData))
        presenter.uiStateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())

            presenter.handleEvent(GameUiEvent.Move(Direction.WEST))
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
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = GamePresenter(createRepository(userData))
        presenter.uiStateFlow.test {
            assertIs<GameUiState.Nothing>(awaitItem())
            presenter.handleEvent(GameUiEvent.Load)
            assertIs<GameUiState.Loading>(awaitItem())
            assertIs<GameUiState.Success>(awaitItem())
            presenter.handleEvent(GameUiEvent.Move(Direction.WEST))
            assertIs<GameUiState.Success>(awaitItem())

            presenter.handleEvent(GameUiEvent.Undo)
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
}

private fun createRepository(
    userData: UserData = UserData.EMPTY_USER_DATA,
): GameRepository = object : GameRepository {
    override suspend fun fetch(): UserData {
        delay(100.milliseconds)
        return userData
    }

    override suspend fun update(data: UserData) {
        delay(100.milliseconds)
    }
}
