package ru.ptrff.photopano.ui.result

sealed interface ResultSideEffects {
    class GenerateAndPasteQR(val url: String) : ResultSideEffects
}
