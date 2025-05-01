package ru.ptrff.photopano.ui.settings

sealed interface SettingsSideEffects {
    class SpanCountChanged(val spanCount: Int) : SettingsSideEffects
}
