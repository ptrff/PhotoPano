package ru.ptrff.photopano.loading.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import ru.ptrff.photopano.MainActivity.Companion.TAG
import ru.ptrff.photopano.loading.presentation.LoadingSideEffects.ChangeAnimationDescription
import ru.ptrff.photopano.loading.presentation.LoadingSideEffects.ProcessingDone
import ru.ptrff.photopano.loading.presentation.LoadingUiEvents.ProcessArguments
import ru.ptrff.photopano.loading.presentation.LoadingUiEvents.StartProcessing
import ru.ptrff.photopano.utils.CameraUtils
import ru.ptrff.photopano.utils.Store
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LoadingStore @Inject constructor(
    val app: Application
) : Store<LoadingState, LoadingSideEffects, LoadingUiEvents>(app) {
    override val _state = MutableStateFlow(LoadingState())
    override val state = _state.asStateFlow()

    override val _sideEffect = Channel<LoadingSideEffects>()
    override val sideEffect = _sideEffect.receiveAsFlow()

    @Inject
    lateinit var cameraUtils: CameraUtils

    override fun onEvent(event: LoadingUiEvents) = when (event) {
        is ProcessArguments -> {
            processArguments(event.arguments ?: error("no arguments provided"))
        }

        is StartProcessing -> {
            startProcessing(event.fileDir)
        }
    }

    private fun processArguments(arguments: Bundle) = arguments.apply {
        state {
            it.copy(
                duration = getFloat("duration", 1f),
                interpolate = getBoolean("interpolate"),
                reverse = getBoolean("reverse"),
                upload = getBoolean("upload")
            )
        }
    }

    @SuppressLint("CheckResult")
    private fun startProcessing(
        fileDir: File
    ) = AnimationUtils(
        state.value.duration ?: error("duration not set"),
        fileDir,
        changeAnimationDescription = { sideEffects(ChangeAnimationDescription(it)) }
    ).apply {
        setCameras(cameraUtils.supportedCameras)

        prepareEnvironment()
            .andThen(Completable.defer(::combineImages))
            .andThen(Completable.defer(::createPalette))
            .andThen(
                Completable.defer {
                    if (state.value.interpolate) {
                        interpolation()
                    } else {
                        Completable.complete()
                    }
                }
            )
            .andThen(
                Completable.defer {
                    if (state.value.reverse) {
                        reverseAnimation()
                    } else {
                        convertToGif()
                    }
                }
            )
            .andThen(Completable.defer(::emptyTemp))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { sideEffects(ProcessingDone(state.value.upload)) },
                onError = { Log.e(TAG, "error creating animation: " + it.message) }
            )
    }

}
