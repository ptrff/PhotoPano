package ru.ptrff.photopano.settings.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.DisplayMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import ru.ptrff.photopano.settings.presentation.SettingsSideEffects.*
import ru.ptrff.photopano.settings.presentation.SettingsUiEvents.*
import ru.ptrff.photopano.utils.CameraUtils
import ru.ptrff.photopano.utils.Store
import ru.ptrff.photopano.utils.fastLazy
import javax.inject.Inject

@HiltViewModel
class SettingsStore @Inject constructor(
    val app: Application
) : Store<SettingsState, SettingsSideEffects, SettingsUiEvents>(app) {
    override val _state = MutableStateFlow(SettingsState())
    override val state = _state.asStateFlow()

    override val _sideEffect = Channel<SettingsSideEffects>()
    override val sideEffect = _sideEffect.receiveAsFlow()

    @Inject
    lateinit var cameraUtils: CameraUtils

    private val sharedPreferences: SharedPreferences by fastLazy {
        app.getSharedPreferences(javaClass.simpleName, Context.MODE_PRIVATE)
    }

    override fun onEvent(event: SettingsUiEvents) = when (event) {
        is Initialize -> {
            calculateSpanCount(event.displayMetrics)
            restorePackCount()
        }

        is DecreasePackCount -> changePackCount(increase = false)
        is IncreasePackCount -> changePackCount(increase = true)
        is SaveSequence -> cameraUtils.saveCameraList(state.value.packCount)
    }

    private fun restorePackCount() = state {
        it.copy(packCount = sharedPreferences.getInt("packCount", it.packCount))
    }

    private fun changePackCount(
        increase: Boolean,
        minPackCount: Int = 1,
        maxPackCount: Int = 9
    ) {
        val packCount = if (increase) state.value.packCount + 1 else state.value.packCount - 1
        state {
            it.copy(packCount = packCount.coerceIn(minPackCount, maxPackCount))
        }
    }

    private fun calculateSpanCount(displayMetrics: DisplayMetrics) {
        val densityDpi = displayMetrics.densityDpi
        val screenHorizontalPadding = 32 * densityDpi / 160
        val itemWidth = 140 * densityDpi / 160
        val gap = 16 * densityDpi / 160
        val spanCount = (displayMetrics.widthPixels - screenHorizontalPadding) / (itemWidth + gap)
        sideEffects(SpanCountChanged(spanCount))
    }
}
