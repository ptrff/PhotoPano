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
fun <T, R, E, V : Store<T, R, E>> LifecycleOwner.initObservers(
    viewModel: V,
    onStateChanged: (T) -> Unit,
    onSideEffect: (R) -> Unit
) = viewModel.viewModelScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.state.collect { onStateChanged(it) } }
        launch { viewModel.sideEffect.collect { onSideEffect(it) } }
    }
}
