package ru.ptrff.photopano.models

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.TextureView
import ru.ptrff.photopano.ui.MainActivity

class Camera(val id: String) {
    val threadName: String = "camera_${id}_thread"
    var textureView: TextureView? = null
    var packId: Int = 1

    lateinit var backgroundThread: HandlerThread
    lateinit var backgroundHandler: Handler
    lateinit var imageReader: ImageReader
    lateinit var previewRequestBuilder: CaptureRequest.Builder
    lateinit var captureRequest: CaptureRequest
    lateinit var cameraDevice: CameraDevice
    lateinit var captureSession: CameraCaptureSession
    lateinit var captureSize: Size
    lateinit var image: Image
    var camNum: Int = 0

    var start: Long = 0
    var end: Long = 0

    var onOpenedCallback: () -> Unit = {
        Log.e(MainActivity.TAG, "onOpenedCallback is not implemented yet")
    }
    var onFrameAcquiredCallback: () -> Unit = {
        Log.e(MainActivity.TAG, "onFrameAcquiredCallback is not implemented yet")
    }
    var onClosedCallback: () -> Unit = {
        Log.e(MainActivity.TAG, "onClosedCallback is not implemented yet")
    }
}
