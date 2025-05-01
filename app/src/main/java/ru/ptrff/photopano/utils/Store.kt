package ru.ptrff.photopano.utils

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class Store<State, SideEffects, UiEvents>(
    app: Application
) : AndroidViewModel(application = app) {
    protected abstract val _state: MutableStateFlow<State>
    abstract val state: StateFlow<State>
    protected abstract val _sideEffect: Channel<SideEffects>
    abstract val sideEffect: Flow<SideEffects>

    abstract fun onEvent(event: UiEvents): Any
}
