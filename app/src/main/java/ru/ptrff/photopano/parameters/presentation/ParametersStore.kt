package ru.ptrff.photopano.parameters.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import ru.ptrff.photopano.models.Camera
import ru.ptrff.photopano.MainActivity.Companion.TAG
import ru.ptrff.photopano.parameters.presentation.ParametersUiEvents.*
import ru.ptrff.photopano.parameters.presentation.ParametersSideEffects.*
import ru.ptrff.photopano.utils.CameraUtils
import ru.ptrff.photopano.utils.Store
import ru.ptrff.photopano.utils.fastLazy
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ParametersStore @Inject constructor(
    val app: Application
) : Store<ParametersState, ParametersSideEffects, ParametersUiEvents>(app) {
    override val _state = MutableStateFlow(ParametersState())
    override val state = _state.asStateFlow()

    override val _sideEffect = Channel<ParametersSideEffects>()
    override val sideEffect = _sideEffect.receiveAsFlow()

    @Inject
    lateinit var cameraUtils: CameraUtils

    private val sharedPreferences: SharedPreferences by fastLazy {
        app.getSharedPreferences(javaClass.simpleName, Context.MODE_PRIVATE)
    }
    private val editor: SharedPreferences.Editor
        get() = sharedPreferences.edit()

    override fun onEvent(event: ParametersUiEvents) = when (event) {
        is Initialize -> initialize()
        is OnShootingDurationChange -> changeShootingDuration(event.value)
        is OnPrepareDurationChange -> changePrepareDuration(event.value)
        is OnInterpolateChange -> changeGifType(event.isActive, state.value.reverse)
        is OnReverseChange -> changeGifType(state.value.interpolate, event.isActive)
        is OnUploadChange -> _state.update { it.copy(upload = event.isActive) }
        is OnDoneClicked -> {
            sideEffects(
                ShowCounterDialog(
                    prepareTime = state.value.prepareDuration,
                    cameraCount = cameraUtils.supportedCameraCount
                )
            )
            prepareCameraQueue()
        }

        is OnCounterDialogDismiss -> cameraUtils::closeAll
        is OnCounterDialogFinish -> startPackingFromQueue()
    }

    private fun initialize() {
        val initDuration = sharedPreferences.getFloat("duration", 1f)
        val initPreparation = sharedPreferences.getInt("preparation", 3)
        _state.update {
            it.copy(
                shootingDuration = initDuration,
                prepareDuration = initPreparation
            )
        }
    }

    private fun changeShootingDuration(value: Float) {
        _state.update { it.copy(shootingDuration = value) }
        editor.putFloat("duration", value).apply()
    }

    private fun changePrepareDuration(value: Int) {
        _state.update { it.copy(prepareDuration = value) }
        editor.putInt("preparation", value.toInt()).apply()
    }

    private fun changeGifType(isInterpolated: Boolean, isReversed: Boolean) = _state.update {
        it.copy(
            interpolate = isInterpolated,
            reverse = isReversed,
            gifType = if (isReversed) {
                if (isInterpolated) GifType.INTERPOLATED_REVERSE else GifType.DEFAULT_REVERSE
            } else {
                if (isInterpolated) GifType.INTERPOLATED else GifType.DEFAULT
            }
        )
    }

    // Camera management
    private val cameraPacks: MutableList<MutableList<Camera>> = ArrayList()
    private val cameraQueue: Queue<Camera> = LinkedList()
    private var cameraQueueLoop: Disposable? = null
    private val intervalStep: Float by fastLazy { 1f / cameraUtils.supportedCameraCount }
    private var interval: Int = 0
    private var iterationNum: Int = 0

    private fun sortCamerasByPack() {
        val cameras = cameraUtils.supportedCameras
        cameraPacks.clear()
        val packCount = cameraUtils.packCount
        repeat(packCount) {
            cameraPacks.add(ArrayList())
        }

        for (i in cameras.indices) {
            val packId = i % packCount
            cameras[i].let { cameraPacks[packId].add(it) }
        }

        Log.d(TAG, "Pack count: ${cameraUtils.packCount}")

        for (packId in 0 until packCount) {
            Log.d(
                TAG,
                "Pack: ${packId + 1} cameras: ${
                    cameraPacks[packId].joinToString(", ") { it.id }
                }"
            )
        }
    }

    @SuppressLint("CheckResult")
    private fun prepareCameraQueue() {
        sortCamerasByPack()
        interval = (state.value.shootingDuration * intervalStep * 1000).toInt()
        interval += 500
        Log.d(TAG, "interval: $interval")

        Completable.fromAction {
            var allPacksEmpty = false
            while (!allPacksEmpty) {
                addToQueue()
                allPacksEmpty = cameraPacks.all { it.isEmpty() }
            }
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { Log.d(TAG, "queue prepared") },
                onError = { it.message?.let { m -> Log.e(TAG, m) } }
            )
    }

    private fun startPackingFromQueue() {
        cameraQueueLoop = Observable
            .interval(interval.toLong(), TimeUnit.MILLISECONDS)
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    if (!cameraQueue.isEmpty()) {
                        val camera = cameraQueue.poll()
                        if (camera != null) {
                            camera.start = System.currentTimeMillis()
                            camera.camNum = iterationNum
                            cameraUtils.startPreview(camera)
                        }
                        iterationNum++
                    }
                },
                onError = {
                    Log.e(TAG, "Camera error: ${it.message}")
                    Log.d(TAG, "Trying continue skipping it")
                    iterationNum++
                    if (iterationNum >= cameraUtils.supportedCameraCount) {
                        cameraQueueLoop?.dispose()
                        sideEffects(ShootingComplete)
                    } else {
                        Log.d(
                            TAG,
                            " iterationNum: $iterationNum cameraCount: ${cameraUtils.supportedCameraCount}"
                        )
                    }
                }
            )
    }

    private fun addToQueue() {
        for (pack in cameraPacks) {
            if (pack.isEmpty()) {
                continue
            }
            val c = pack.removeAt(0)
            enqueueCamera(c)

            Log.d(
                TAG,
                "added to queue: ${c.id} from pack: ${cameraPacks.indexOf(pack)}"
            )
        }
    }

    private fun enqueueCamera(camera: Camera) = cameraUtils.openCapture(
        camera = camera,
        onOpenedCallback = {
            cameraQueue.offer(camera)
        },
        onFrameAcquiredCallback = {
            sideEffects(NextShootingStep)
        },
        onClosedCallback = {
            camera.end = System.currentTimeMillis()
            Log.d(
                TAG,
                "camera ${camera.id} operated for ${camera.end - camera.start} ms"
            )
            if (cameraQueue.isEmpty() && iterationNum == cameraUtils.supportedCameraCount) {
                cameraQueueLoop?.dispose()
                _sideEffect.trySend(ShootingComplete)
            }
        }
    )
}
