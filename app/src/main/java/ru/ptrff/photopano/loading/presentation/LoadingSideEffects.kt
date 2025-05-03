package ru.ptrff.photopano.loading.presentation

sealed interface LoadingSideEffects {
    class ChangeAnimationDescription(val stringRes: Int) : LoadingSideEffects
    class ProcessingDone(val upload: Boolean) : LoadingSideEffects
}
