import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.alexjlockwood.twentyfortyeight.domain.Cell
import com.alexjlockwood.twentyfortyeight.domain.Direction
import com.alexjlockwood.twentyfortyeight.domain.Tile
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.ui.GameUiEvent
import com.alexjlockwood.twentyfortyeight.ui.GameUiState
import com.alexjlockwood.twentyfortyeight.ui.gamePresenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class PresenterTest {

    @Test
    fun loadEmptyUserData() = runTest {
        val presenter = createPresenter(createRepository())
        val owner = createLifecycleAndViewModelStoreOwner()
        moleculeFlow(RecompositionMode.Immediate) {
            owner.returningCompositionLocalProvider {
                presenter.presenter()
            }
        }.test {
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }
        }
    }

    @Test
    fun loadSavedUserData() = runTest {
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(userData))
        val owner = createLifecycleAndViewModelStoreOwner()
        moleculeFlow(RecompositionMode.Immediate) {
            owner.returningCompositionLocalProvider {
                presenter.presenter()
            }
        }.test {
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
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(userData))
        val owner = createLifecycleAndViewModelStoreOwner()
        moleculeFlow(RecompositionMode.Immediate)  {
            owner.returningCompositionLocalProvider { presenter.presenter() }
        }.test {
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }

            presenter.eventSink(GameUiEvent.StartNewGame)
            val item = awaitItem()
            assertTrue { item is GameUiState.Success }
            item as GameUiState.Success
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
        val presenter = createPresenter(createRepository(userData))
        val owner = createLifecycleAndViewModelStoreOwner()
        moleculeFlow(RecompositionMode.Immediate)  {
            owner.returningCompositionLocalProvider { presenter.presenter() }
        }.test {
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }

            presenter.eventSink(GameUiEvent.Move(Direction.WEST))
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
        val userData = UserData.EMPTY_USER_DATA.copy(
            grid = List(4) { List(4) { Tile(16) } },
            currentScore = 16,
            bestScore = 32,
        )
        val presenter = createPresenter(createRepository(userData))
        val owner = createLifecycleAndViewModelStoreOwner()
        moleculeFlow(RecompositionMode.Immediate)  {
            owner.returningCompositionLocalProvider { presenter.presenter() }
        }.test {
            assertTrue { awaitItem() is GameUiState.Loading }
            assertTrue { awaitItem() is GameUiState.Success }
            presenter.eventSink(GameUiEvent.Move(Direction.WEST))
            assertTrue { awaitItem() is GameUiState.Success }

            presenter.eventSink(GameUiEvent.Undo)
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

private interface Presenter<STATE, EVENT> {

    @Composable
    fun presenter(): STATE

    fun eventSink(event: EVENT)
}

private fun createPresenter(
    repository: GameRepository = createRepository(),
): Presenter<GameUiState, GameUiEvent> = object : Presenter<GameUiState, GameUiEvent> {
    private val eventFlow = MutableSharedFlow<GameUiEvent>(extraBufferCapacity = 20)

    @Composable
    override fun presenter(): GameUiState = gamePresenter(eventFlow, repository)

    override fun eventSink(event: GameUiEvent) {
        eventFlow.tryEmit(event)
    }
}
