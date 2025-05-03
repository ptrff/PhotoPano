package ru.ptrff.photopano.parameters.presentation

sealed interface ParametersSideEffects {
    class ShowCounterDialog(val prepareTime: Int, val cameraCount: Int) : ParametersSideEffects
    class ShootingComplete(val state: ParametersState) : ParametersSideEffects
    object NextShootingStep : ParametersSideEffects
}
