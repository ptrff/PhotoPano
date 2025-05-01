package ru.ptrff.photopano.settings.presentation

sealed interface SettingsSideEffects {
    class SpanCountChanged(val spanCount: Int) : SettingsSideEffects
}
