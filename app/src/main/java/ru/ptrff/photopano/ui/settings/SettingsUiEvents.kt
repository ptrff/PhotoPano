package ru.ptrff.photopano.ui.settings

import android.util.DisplayMetrics

sealed class SettingsUiEvents {
    class Initialize(val displayMetrics: DisplayMetrics) : SettingsUiEvents()
    object IncreasePackCount : SettingsUiEvents()
    object DecreasePackCount : SettingsUiEvents()
    object SaveSequence : SettingsUiEvents()
}
