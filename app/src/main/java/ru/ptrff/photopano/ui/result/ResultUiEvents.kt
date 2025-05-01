package ru.ptrff.photopano.ui.result

import java.io.File

sealed class ResultUiEvents {
    class UploadGif(val file: File) : ResultUiEvents()
}
