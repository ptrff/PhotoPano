package ru.ptrff.photopano.ui.result

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import ru.ptrff.photopano.ui.result.ResultSideEffects.GenerateAndPasteQR
import ru.ptrff.photopano.utils.Uploader
import java.io.File

class ResultViewModel(val app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ResultState())
    val state: StateFlow<ResultState> = _state.asStateFlow()

    private val _sideEffect = Channel<ResultSideEffects>()
    val sideEffect = _sideEffect.receiveAsFlow()

    fun onEvent(event: ResultUiEvents) = when (event) {
        is ResultUiEvents.UploadGif -> upload(event.file)
    }

    private fun upload(file: File) = Uploader().apply {
        upload(
            app,
            file,
            object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState) {
                    if (TransferState.COMPLETED == state) {
                        _sideEffect.trySend(GenerateAndPasteQR(fileUrl.toString()))
                        _state.update { it.copy(progress = 100) }
                        Log.d("Uploader", "uplodaing complete")
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    val percent = ((bytesCurrent.toFloat() / bytesTotal.toFloat()) * 100).toInt()
                    _state.update { it.copy(progress = percent) }
                    Log.d("Uploader", "uplodaing: $percent%")
                }

                override fun onError(id: Int, ex: Exception) {
                    _state.update { it.copy(progress = -1) }
                    Log.e("Uploader", "Error during upload", ex)
                }
            }
        )
    }
}
