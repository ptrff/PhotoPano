package ru.ptrff.photopano.utils

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch

/**
 * A delegate for a [ViewBinding] property in an [AppCompatActivity].
 */
inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) = lazy(LazyThreadSafetyMode.NONE) {
    bindingInflater.invoke(layoutInflater)
}

/**
 * A delegate for a [ViewBinding] property in an [Fragment].
 */
inline fun <T : ViewBinding> Fragment.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) = lazy(LazyThreadSafetyMode.NONE) {
    bindingInflater.invoke(layoutInflater)
}

/**
 * A delegate for fast lazy initialization
 */
inline fun <T> fastLazy(
    crossinline operation: () -> T
) = lazy(LazyThreadSafetyMode.NONE) {
    operation()
}

/**
 * An extension fun for quickly subscribe to [Store] state and side effects flows
 * inside [LifecycleOwner] (e.g. [Fragment])
 */
fun <State, SideEffects, UiEvents, S : Store<State, SideEffects, UiEvents>> LifecycleOwner.initObservers(
    store: S,
    onStateChanged: (State) -> Unit,
    onSideEffect: (SideEffects) -> Unit
) = store.viewModelScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { store.state.collect { onStateChanged(it) } }
        launch { store.sideEffect.collect { onSideEffect(it) } }
    }
}
