package ru.ptrff.photopano.result.presentation

sealed interface ResultSideEffects {
    class GenerateAndPasteQR(val url: String) : ResultSideEffects
}
