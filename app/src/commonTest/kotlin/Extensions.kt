import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.currentComposer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

interface LifecycleAndViewModelStoreOwner : LifecycleOwner, ViewModelStoreOwner

fun createLifecycleAndViewModelStoreOwner(
    initialState: Lifecycle.State = Lifecycle.State.STARTED,
): LifecycleAndViewModelStoreOwner = object : LifecycleAndViewModelStoreOwner {

    override val lifecycle: LifecycleRegistry
        get() = LifecycleRegistry.createUnsafe(this).apply {
            currentState = initialState
        }
    override val viewModelStore: ViewModelStore
        get() = ViewModelStore()
}

@OptIn(InternalComposeApi::class)
@Composable
fun <T> returningCompositionLocalProvider(
    vararg values: ProvidedValue<*>,
    content: @Composable () -> T,
): T {
    currentComposer.startProviders(values)
    val result = content()
    currentComposer.endProviders()
    return result
}

@Composable
fun <T> LifecycleAndViewModelStoreOwner.returningCompositionLocalProvider(
    content: @Composable () -> T,
): T {
    return returningCompositionLocalProvider(
        LocalLifecycleOwner provides this,
        LocalViewModelStoreOwner provides this,
        content = content,
    )
}
