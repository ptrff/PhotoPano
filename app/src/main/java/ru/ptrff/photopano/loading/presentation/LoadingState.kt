package ru.ptrff.photopano.loading.presentation

data class LoadingState(
    val duration: Float? = null,
    val interpolate: Boolean = false,
    val reverse: Boolean = false,
    val upload: Boolean = false,
)
