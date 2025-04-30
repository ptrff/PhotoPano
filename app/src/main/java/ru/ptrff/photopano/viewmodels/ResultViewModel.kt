package ru.ptrff.photopano.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import ru.ptrff.photopano.utils.Uploader
import java.io.File

class ResultViewModel(val app: Application) : AndroidViewModel(app) {
    val progress: MutableLiveData<Int> = MutableLiveData(0)
    val url: MutableLiveData<String> = MutableLiveData()

    fun upload(file: File) = Uploader().apply {
        upload(
            app,
            file,
            object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState) {
                    if (TransferState.COMPLETED == state) {
                        url.value = fileUrl.toString()
                        progress.postValue(100)
                        Log.d("Uploader", "uplodaing complete")
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    val percentDone =
                        ((bytesCurrent.toFloat() / bytesTotal.toFloat()) * 100).toInt()
                    progress.postValue(percentDone)
                    Log.d("Uploader", "uplodaing: $percentDone%")
                }

                override fun onError(id: Int, ex: Exception) {
                    progress.postValue(-1)
                    Log.e("Uploader", "Error during upload", ex)
                }
            }
        )
    }
}
