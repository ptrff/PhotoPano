package ru.ptrff.photopano.utils

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

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
