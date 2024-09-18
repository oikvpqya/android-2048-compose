// https://github.com/takahirom/Rin 0.1.0 Apache-2.0 license by takahirom

package io.github.takahirom.rin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

var RIN_DEBUG = false

private class ProduceRetainedStateScopeImpl<T>(
    state: MutableState<T>,
    override val coroutineContext: CoroutineContext,
) : ProduceStateScope<T>, MutableState<T> by state {

    override suspend fun awaitDispose(onDispose: () -> Unit): Nothing {
        try {
            suspendCancellableCoroutine<Nothing> {}
        } finally {
            onDispose()
        }
    }
}

@Composable
public fun <T> produceRetainedState(
    initialValue: T,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = rememberRetained { mutableStateOf(initialValue) }
    LaunchedEffect(Unit) { ProduceRetainedStateScopeImpl(result, coroutineContext).producer() }
    return result
}

@Composable
public fun <T : R, R> Flow<T>.collectAsRetainedState(
    initial: R,
    context: CoroutineContext = EmptyCoroutineContext,
): State<R> =
    produceRetainedState(initial, this, context) {
        if (context == EmptyCoroutineContext) {
            collect { value = it }
        } else withContext(context) { collect { value = it } }
    }

@Composable
public fun <T> produceRetainedState(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = rememberRetained { mutableStateOf(initialValue) }
    LaunchedEffect(key1, key2) { ProduceRetainedStateScopeImpl(result, coroutineContext).producer() }
    return result
}

@Composable
public fun <T> StateFlow<T>.collectAsRetainedState(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsRetainedState(value, context)

interface RetainedObserver {
    fun onRemembered()

    fun onForgotten()
}

val LocalShouldRemoveRetainedWhenRemovingComposition = compositionLocalOf<(LifecycleOwner) -> Boolean> {
    { lifecycleOwner ->
        val state = lifecycleOwner.lifecycle.currentState
        state == Lifecycle.State.RESUMED
    }
}

@Composable
fun <T : Any> rememberRetained(
    key: String? = null,
    block: @DisallowComposableCalls () -> T,
): T {
    // Caution: currentCompositeKeyHash is not unique so we need to store multiple values with the same key
    val keyToUse: String = key ?: currentCompositeKeyHash.toString(36)
    val viewModelFactory = remember {
        viewModelFactory {
            addInitializer(RinViewModel::class) { RinViewModel() }
        }
    }
    val rinViewModel: RinViewModel = viewModel(modelClass = RinViewModel::class, factory = viewModelFactory)
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleOwnerHash = lifecycleOwner.hashCode().toString(36)
    val removeRetainedWhenRemovingComposition = LocalShouldRemoveRetainedWhenRemovingComposition.current

    val result = remember(lifecycleOwner, keyToUse) {
        log { "rememberRetained: remember $keyToUse" }
        val consumedValue = rinViewModel.consume(keyToUse)

        @Suppress("UNCHECKED_CAST")
        val result = consumedValue ?: block()
        rinViewModel.onRestoreOrCreate(keyToUse)

        object : RememberObserver {
            val result = result
            override fun onAbandoned() {
                onForgot()
            }

            override fun onForgotten() {
                onForgot()
            }

            fun onForgot() {
                log { "RinViewModel: rememberRetained: onForgot $keyToUse lifecycleOwner:$lifecycleOwner lifecycleOwner.lifecycle.currentState:${lifecycleOwner.lifecycle.currentState}" }
                rinViewModel.onForget(keyToUse, removeRetainedWhenRemovingComposition(lifecycleOwner))
            }

            override fun onRemembered() {
                rinViewModel.onRemembered(keyToUse, result, consumedValue != null)
            }
        }
    }.result as T
    SideEffect {
        rinViewModel.onNewSideEffect(removeRetainedWhenRemovingComposition(lifecycleOwner), lifecycleOwnerHash)
    }
    return result
}

internal class RinViewModel : ViewModel() {

    internal val savedData = mutableMapOf<String, ArrayDeque<RinViewModelEntity<Any?>>>()
    private val rememberedData = mutableMapOf<String, ArrayDeque<RinViewModelEntity<Any?>>>()

    init {
        log { "RinViewModel($this): created" }
    }

    fun consume(key: String): Any? {
        val value = (savedData[key])?.removeFirstOrNull()?.value
        log { "RinViewModel($this): consume key:$key value:$value savedData:$savedData" }
        return value
    }

    fun onRestoreOrCreate(key: String) {
        val entity = savedData[key]
        entity?.forEach {
            it.onRestore()
        }
        log { "RinViewModel: onRestoreOrCreate $key" }
    }

    fun onRemembered(key: String, value: Any, isRestored: Boolean) {
        val element: RinViewModelEntity<Any?> = RinViewModelEntity(
            value = value,
            hasBeenRestored = isRestored
        )
        rememberedData.getOrPut(key) { ArrayDeque() }.add(
            element
        )

        element.onRemember()

        log { "RinViewModel: onRemembered key:$key element:$element isRestored:$isRestored" }
    }

    override fun onCleared() {
        super.onCleared()
        val tmp = savedData.toList()
        rememberedData.clear()
        clearSavedData()
        log { "RinViewModel($this): onCleared removed:$tmp" }
    }

    fun onForget(key: String, canRemove: Boolean) {
        log {
            "RinViewModel($this): onForget key:$key canRemove:$canRemove isInRemember{${rememberedData.contains(key)}} isInSaved:{${
                savedData.contains(
                    key
                )
            }}"
        }
        if (!canRemove) {
            return
        }
        val entity = savedData[key]
        entity?.forEach {
            it.close()
        }
        savedData.remove(key)
    }

    private var lastLifecycleOwnerHash = ""

    fun onNewSideEffect(canRemove: Boolean, lifecycleOwnerHash: String) {
        if (rememberedData.isEmpty()) {
            return
        }
        val tmp = savedData.toList()
        if (canRemove && lastLifecycleOwnerHash != lifecycleOwnerHash) {
            // If recomposition we don't remove the saved data
            lastLifecycleOwnerHash = lifecycleOwnerHash
            clearSavedData()
        }
        savedData.putAll(rememberedData)
        rememberedData.clear()
        log { "RinViewModel: onSideEffect savedData:$savedData rememberedData:$rememberedData removed:$tmp" }
    }

    private fun clearSavedData() {
        savedData.values.forEach {
            it.forEach {
                it.close()
            }
        }
        savedData.clear()
    }

    data class RinViewModelEntity<T>(
        var value: T,
        var hasBeenRestored: Boolean = false,
    ) {

        fun onRestore() {
            hasBeenRestored = true
        }

        fun onRemember() {
            if (hasBeenRestored) {
                return
            }
            val v = value ?: return
            when (v) {
                is RetainedObserver -> v.onRemembered()
            }
        }

        fun close() {
            onForgot()
        }

        private fun onForgot() {
            val v = value ?: return
            when (v) {
                is RetainedObserver -> v.onForgotten()
            }
        }
    }
}

internal fun log(msgBlock: () -> Any) {
    if (!RIN_DEBUG) {
        return
    }
    println(msgBlock())
}

internal fun log(msg: Any) {
    if (!RIN_DEBUG) {
        return
    }
    println(msg)
}
