import androidx.compose.runtime.mutableIntStateOf
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import io.github.takahirom.rin.RetainedObserver
import io.github.takahirom.rin.rememberRetained
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class RememberRetainedTest {

    @Test
    fun rememberRetained() = runTest {
        val owner = createLifecycleAndViewModelStoreOwner()
        moleculeFlow(RecompositionMode.Immediate)  {
            owner.returningCompositionLocalProvider {
                rememberRetained {
                    object : RetainedObserver {
                        val scope = CoroutineScope(Dispatchers.Default)
                        val state = mutableIntStateOf(0)
                        override fun onForgotten() {
                            scope.cancel()
                        }

                        override fun onRemembered() {
                            scope.launch {
                                delay(100.milliseconds)
                                state.value = 1
                            }
                        }
                    }
                }.state.value
            }
        }.test {
            assertEquals(0, awaitItem())
            assertEquals(1, awaitItem())
        }
    }
}
