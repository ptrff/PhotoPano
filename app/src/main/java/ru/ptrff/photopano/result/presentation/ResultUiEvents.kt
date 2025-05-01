package ru.ptrff.photopano.result.presentation

import java.io.File

sealed class ResultUiEvents {
    class UploadGif(val file: File) : ResultUiEvents()
}
