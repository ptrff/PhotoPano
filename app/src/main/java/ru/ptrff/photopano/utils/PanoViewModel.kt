package ru.ptrff.photopano.utils

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class PanoViewModel<T, R>(
    app: Application
) : AndroidViewModel(application = app) {
    protected abstract val _state: MutableStateFlow<T>
    abstract val state: StateFlow<T>
    protected abstract val _sideEffect: Channel<R>
    abstract val sideEffect: Flow<R>
}
