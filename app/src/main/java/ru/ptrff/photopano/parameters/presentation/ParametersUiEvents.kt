package ru.ptrff.photopano.parameters.presentation

sealed class ParametersUiEvents {
    object Initialize: ParametersUiEvents()
    class OnShootingDurationChange(val value: Float) : ParametersUiEvents()
    class OnPrepareDurationChange(val value: Int) : ParametersUiEvents()
    class OnInterpolateChange(val isActive: Boolean) : ParametersUiEvents()
    class OnReverseChange(val isActive: Boolean) : ParametersUiEvents()
    class OnUploadChange(val isActive: Boolean) : ParametersUiEvents()
    object OnDoneClicked : ParametersUiEvents()
    object OnCounterDialogDismiss : ParametersUiEvents()
    object OnCounterDialogFinish : ParametersUiEvents()
}
