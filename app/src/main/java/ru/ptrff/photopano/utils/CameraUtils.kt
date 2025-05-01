package ru.ptrff.photopano.utils

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ru.ptrff.photopano.models.Camera
import ru.ptrff.photopano.MainActivity
import ru.ptrff.photopano.settings.presentation.SettingsStore
import java.io.File
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraUtils @Inject constructor(
    val app: Application
) {

    private val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraList: MutableList<Camera> = ArrayList()

    private val sharedPreferences: SharedPreferences by fastLazy {
        app.getSharedPreferences(SettingsStore::class.simpleName, Context.MODE_PRIVATE)
    }
    private val editor: SharedPreferences.Editor by fastLazy {
        sharedPreferences.edit()
    }

    private val temp = File(app.filesDir, "temp")
    var packCount: Int = 1
        private set

    fun init() {
        if (!temp.exists()) temp.mkdirs()

        if (ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                for (id in cameraManager.cameraIdList) {
                    val camera = initCamera(id)
                    cameraList.add(camera)
                }
            } catch (e: CameraAccessException) {
                e.message?.let { Log.e(MainActivity.TAG, it) }
            }
        }

        restoreCameraList()
    }

    fun saveCameraList(packCount: Int) {
        this.packCount = packCount
        editor.putInt("packCount", packCount)

        editor.putString(
            "cameralist",
            cameraList
                .stream()
                .map { camera -> camera.id }
                .collect(Collectors.joining(","))
        )
        editor.apply()
        Log.d(MainActivity.TAG, "Camera list saved, pack count: $packCount")
    }

    fun restoreCameraList() {
        if (!sharedPreferences.contains("cameralist")) {
            return
        }

        packCount = sharedPreferences.getInt("packCount", 1)

        val ids = sharedPreferences.getString("cameralist", "")!!
            .split(",")
            .filterNot { it.isEmpty() }
        val currentListIds = cameraList.map { camera -> camera.id }.toMutableList()

        Log.d(MainActivity.TAG, "Restoring camera list: ")

        var notFountCount = 0
        for (i in ids.indices) {
            if (currentListIds.contains(ids[i])) {
                val currentIndex = currentListIds.indexOf(ids[i])
                val camera = cameraList[currentIndex]
                cameraList.removeAt(currentIndex)
                currentListIds.removeAt(currentIndex)
                cameraList.add(i - notFountCount, camera)
                currentListIds.add(i - notFountCount, camera.id)
                Log.d(MainActivity.TAG, "    Restored position of ${camera.id}")
            } else {
                notFountCount++
                Log.d(MainActivity.TAG, "    Skipped count: $notFountCount")
            }
        }
    }

    private fun initCamera(id: String): Camera {
        val camera = Camera(id)

        camera.captureSize = Size(1920, 1080)

        val handlerThread = HandlerThread(camera.threadName).also { it.start() }
        val handler = Handler(handlerThread.looper)
        camera.backgroundThread = handlerThread
        camera.backgroundHandler = handler
        return camera
    }

    fun reInit() {
        cameraList.clear()
        init()
    }

    fun openCapture(
        camera: Camera,
        onOpenedCallback: () -> Unit,
        onFrameAcquiredCallback: () -> Unit,
        onClosedCallback: () -> Unit,
    ) {
        camera.onOpenedCallback = onOpenedCallback
        camera.onFrameAcquiredCallback = onFrameAcquiredCallback
        camera.onClosedCallback = onClosedCallback
        open(camera, null)
    }

    fun openPreview(
        camera: Camera,
        textureView: TextureView,
        onOpenedCallback: () -> Unit,
        onFrameAcquiredCallback: () -> Unit,
        onClosedCallback: () -> Unit
    ) {
        camera.onOpenedCallback = onOpenedCallback
        camera.onFrameAcquiredCallback = onFrameAcquiredCallback
        camera.onClosedCallback = onClosedCallback
        open(camera, textureView)
    }

    private fun open(camera: Camera, textureView: TextureView?) {
        textureView?.let {
            if (ActivityCompat.checkSelfPermission(
                    textureView.context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        try {
            camera.textureView = textureView
            cameraManager.openCamera(camera.id, stateCallback, camera.backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(MainActivity.TAG, "has no permission to access camera: ${e.message}")
        }
    }

    fun startPreview(camera: Camera) {
        val builder: CaptureRequest.Builder

        val outputs: MutableList<Surface> = ArrayList()
        if (camera.textureView != null) {
            val texture = camera.textureView?.surfaceTexture ?: return
            texture.setDefaultBufferSize(
                camera.captureSize.width,
                camera.captureSize.height
            )

            camera.textureView?.surfaceTextureListener = object : SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) = Unit

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) = Unit

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    camera.onFrameAcquiredCallback()
                }
            }

            if (texture.isReleased) return
            val surface = Surface(texture)
            outputs.add(surface)

            try {
                builder = camera.cameraDevice?.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
                ) ?: run {
                    error("no camera device")
                    return
                }
            } catch (e: CameraAccessException) {
                Log.e(MainActivity.TAG, "camera " + camera.id + " error: " + e.message)
                return
            }
            builder.addTarget(surface)
        } else {
            val imageReader = ImageReader.newInstance(
                camera.captureSize.width,
                camera.captureSize.height,
                ImageFormat.YUV_420_888,
                1
            )
            imageReader.setOnImageAvailableListener({
                Log.d(MainActivity.TAG, "image available on " + camera.id)
                val tempFile = File(temp, camera.camNum.toString() + ".jpg")
                camera.backgroundHandler.post(
                    ImageSaver(it.acquireLatestImage(), tempFile) {
                        Log.d(MainActivity.TAG, "image from ${camera.id} saved as ${tempFile.name}")
                        camera.onFrameAcquiredCallback()
                        close(camera)
                    }
                )
            }, camera.backgroundHandler)

            camera.imageReader = imageReader
            outputs.add(imageReader.surface)

            try {
                builder = camera.cameraDevice?.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                ) ?: run {
                    error("no camera device")
                    return
                }
            } catch (e: CameraAccessException) {
                Log.e(MainActivity.TAG, "camera ${camera.id} error: ${e.message}")
                return
            }
            builder.addTarget(imageReader.surface)
        }

        camera.previewRequestBuilder = builder

        try {
            camera.cameraDevice?.createCaptureSession(
                outputs,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        camera.captureSession = session

                        builder.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED
                        )
                        builder.set(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_STATE_PRECAPTURE
                        )
                        builder.set(
                            CaptureRequest.NOISE_REDUCTION_MODE,
                            CaptureRequest.NOISE_REDUCTION_MODE_OFF
                        )
                        builder.set(
                            CaptureRequest.EDGE_MODE,
                            CaptureRequest.EDGE_MODE_OFF
                        )

                        val request = builder.build()
                        camera.captureRequest = request

                        try {
                            if (camera.textureView != null) {
                                session.setRepeatingRequest(
                                    request,
                                    null,
                                    camera.backgroundHandler
                                )
                            } else {
                                session.capture(
                                    request,
                                    null,
                                    camera.backgroundHandler
                                )
                            }
                        } catch (e: CameraAccessException) {
                            Log.e(MainActivity.TAG, "camera " + camera.id + " error: " + e.message)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(MainActivity.TAG, "camera " + camera.id + " configure failed")
                    }
                },
                camera.backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(MainActivity.TAG, "camera " + camera.id + " session error: " + e.message)
        }
    }

    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            Log.d(MainActivity.TAG, "camera ${cameraDevice.id} opened")
            val camera = getCameraById(cameraDevice.id) ?: return
            camera.cameraDevice = cameraDevice
            camera.onOpenedCallback()
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            super.onClosed(cameraDevice)
            val camera = getCameraById(cameraDevice.id) ?: return
            camera.onClosedCallback()
            Log.d(MainActivity.TAG, "camera ${cameraDevice.id} closed")
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Log.d(MainActivity.TAG, "camera ${cameraDevice.id} disconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(MainActivity.TAG, "camera ${camera.id} error: $error")
        }
    }

    fun releaseImageResources(camera: Camera) = camera.apply {
        imageReader.close()
        image.close()
        textureView = null
    }

    fun close(camera: Camera) = camera.apply {
        captureSession?.close()
        cameraDevice?.close()
    }

    fun closeAll() = cameraList.forEach(::close)

    fun releaseAll() = cameraList.forEach(::releaseImageResources)

    private fun getCameraById(id: String): Camera? = cameraList.firstOrNull { it.id == id }

    val supportedCameraCount: Int
        get() = supportedCameras.size

    val cameraCount: Int
        get() = cameraList.size

    val supportedCameras: List<Camera>
        get() = cameraList
}
