package ru.ptrff.photopano.loading.presentation

import android.os.Bundle
import java.io.File

sealed class LoadingUiEvents {
    class ProcessArguments(val arguments: Bundle?) : LoadingUiEvents()
    class StartProcessing(val fileDir: File) : LoadingUiEvents()
}
