package ru.ptrff.photopano.ui.parameters

sealed interface ParametersSideEffects {
    class ShowCounterDialog(val prepareTime: Int, val cameraCount: Int) : ParametersSideEffects
    object ShootingComplete : ParametersSideEffects
    object NextShootingStep : ParametersSideEffects
}
