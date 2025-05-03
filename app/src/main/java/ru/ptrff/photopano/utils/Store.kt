package ru.ptrff.photopano.utils

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class Store<State, SideEffects, UiEvents>(
    app: Application
) : AndroidViewModel(application = app) {
    protected abstract val _state: MutableStateFlow<State>
    abstract val state: StateFlow<State>
    protected abstract val _sideEffect: Channel<SideEffects>
    abstract val sideEffect: Flow<SideEffects>

    abstract fun onEvent(event: UiEvents): Any

    fun sideEffects(vararg sideEffects: SideEffects) = sideEffects.forEach {
        viewModelScope.launch {
            _sideEffect.send(it)
        }
    }

    fun state(newState: (State) -> State) = _state.update { newState(it) }
}
