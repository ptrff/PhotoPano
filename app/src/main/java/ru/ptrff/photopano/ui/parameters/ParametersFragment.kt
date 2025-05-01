package ru.ptrff.photopano.ui.parameters

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.R
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import pl.droidsonroids.gif.GifDrawable
import ru.ptrff.photopano.databinding.FragmentParametersBinding
import ru.ptrff.photopano.models.Camera
import ru.ptrff.photopano.ui.MainActivity.Companion.TAG
import ru.ptrff.photopano.utils.CameraUtils
import ru.ptrff.photopano.utils.fastLazy
import ru.ptrff.photopano.utils.viewBinding
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ParametersFragment : Fragment() {
    private val binding by viewBinding(FragmentParametersBinding::inflate)
    private lateinit var counterDialog: CounterDialog
    private val sharedPreferences: SharedPreferences by fastLazy {
        requireActivity().getPreferences(Context.MODE_PRIVATE)
    }
    private val editor: SharedPreferences.Editor
        get() = sharedPreferences.edit()

    @Inject
    lateinit var cameraUtils: CameraUtils

    val cameraCount: Int by fastLazy {
        cameraUtils.supportedCameraCount
    }
    private var intervalStep: Float = 0f
    private val minDuration: Float = 0.5f
    private val maxDuration: Float = 5f
    private val durationStep: Float = 0.5f
    private var interval: Int = 0
    private var iterationNum: Int = 0

    private var sampleGifDrawable: GifDrawable? = null

    private val cameraPacks: MutableList<MutableList<Camera>> = ArrayList()
    private val cameraQueue: Queue<Camera> = LinkedList()
    private var cameraQueueLoop: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClicks()
        initSliders()
        initSampleAnimation(GifType.DEFAULT_REVERSE)
    }

    private fun initClicks() = with(binding) {
        back.setOnClickListener { it.findNavController().popBackStack() }

        durationSlider.addOnChangeListener { _: Slider, value: Float, fromUser: Boolean ->
            if (fromUser) {
                intervalValue.text = String.Companion.format(
                    Locale.US, "%.2f",
                    value * intervalStep
                )
                durationValue.text = String.Companion.format(
                    Locale.US, "%.2f",
                    value
                )
                val rev = if (binding.reverse.isChecked) 0.5f else 1f
                sampleGifDrawable?.let {
                    it.setSpeed((it.duration / (value * durationStep * 1000)) * rev)
                }
                editor.putFloat("duration", value).apply()
            }
        }

        preparationSlider.addOnChangeListener { _: Slider, value: Float, fromUser: Boolean ->
            if (fromUser) {
                preparationValue.text = value.toInt().toString()
                editor.putInt("preparation", value.toInt()).apply()
            }
        }

        done.setOnClickListener {
            done.isEnabled = false
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(
                R.attr.colorOnTertiary,
                typedValue,
                true
            )
            done.setBackgroundColor(typedValue.data)

            prepareCameraQueue()
            showDialog(typedValue)
        }

        reverse.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            changeGifType(interpolate.isChecked, isChecked)
        }

        interpolate.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            changeGifType(reverse.isChecked, isChecked)
        }
    }

    private fun showDialog(typedValue: TypedValue) {
        counterDialog = CounterDialog(
            binding.preparationSlider.value.toInt(),
            cameraCount,
            layoutInflater
        ).apply {
            setOnDismissListener {
                binding.done.isEnabled = true
                this@ParametersFragment.requireContext().theme.resolveAttribute(
                    R.attr.colorTertiaryContainer,
                    typedValue,
                    true
                )
                binding.done.setBackgroundColor(typedValue.data)
                cameraUtils.closeAll()
            }

            fadeOutParametersCallback = { duration: Int ->
                binding.parametersRoot.animate()
                    .alpha(0f)
                    .setInterpolator(LinearInterpolator())
                    .setDuration(duration.toLong())
                    .start()
            }

            startShootingCallback = {
                binding.flashes.animate()
                    .alpha(0.4f)
                    .setInterpolator(LinearInterpolator())
                    .setDuration(1000)
                    .start()

                startFlashes()
                startPackingFromQueue()
            }

            show()
        }
    }

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

        interval = (binding.durationSlider.value * intervalStep * 1000).toInt()
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
                    if (iterationNum >= cameraCount) {
                        cameraQueueLoop?.dispose()
                        binding.root.post(::shootingComplete)
                    } else {
                        Log.d(
                            TAG,
                            " iterationNum: $iterationNum cameraCount: $cameraCount"
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
            binding.root.post { counterDialog.nextStep() }
        },
        onClosedCallback = {
            camera.end = System.currentTimeMillis()
            Log.d(
                TAG,
                "camera ${camera.id} operated for ${camera.end - camera.start} ms"
            )
            if (cameraQueue.isEmpty() && iterationNum == cameraCount) {
                cameraQueueLoop?.dispose()
                binding.root.post(::shootingComplete)
            }
        }
    )

    private fun shootingComplete() {
        counterDialog.counterComplete()
        Bundle().apply {
            putFloat("duration", binding.durationSlider.value)
            putBoolean("interpolate", binding.interpolate.isChecked)
            putBoolean("reverse", binding.reverse.isChecked)
            putBoolean("upload", binding.upload.isChecked)
            binding.back.findNavController()
                .navigate(ru.ptrff.photopano.R.id.action_global_loadingFragment, this)
        }
    }

    private fun startFlashes() {
        val drawable = RotatedGradientDrawable(
            requireContext(),
            intArrayOf(
                requireContext().getColor(android.R.color.white),
                requireContext().getColor(ru.ptrff.photopano.R.color.white_alpha_02),
                requireContext().getColor(ru.ptrff.photopano.R.color.md_theme_background_transparent)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            binding.flashes.measuredWidth,
            binding.flashes.measuredHeight
        )
        binding.flashes.background = drawable

        ObjectAnimator.ofFloat(0f, 360f).apply {
            setDuration(3600L * cameraCount)
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animator: ValueAnimator ->
                drawable.setRotationDegrees(animator.animatedValue as Float)
            }
            start()
        }
    }

    private fun initSliders() = with(binding) {
        intervalStep = 1f / cameraCount

        durationSlider.valueFrom = minDuration
        durationSlider.valueTo = maxDuration
        durationSlider.stepSize = durationStep

        // set defaults
        val initDuration = sharedPreferences.getFloat("duration", 1f)
        durationSlider.value = initDuration
        durationValue.text = initDuration.toString()
        intervalValue.text = String.Companion.format(
            Locale.US, "%.2f",
            initDuration * intervalStep
        )

        val initPreparation = sharedPreferences.getInt("preparation", 5)
        preparationSlider.value = initPreparation.toFloat()
        preparationValue.text = initPreparation.toString()
    }

    private fun changeGifType(isInterpolated: Boolean, isReversed: Boolean) {
        val tempType = if (isReversed) {
            if (isInterpolated) GifType.INTERPOLATED_REVERSE else GifType.DEFAULT_REVERSE
        } else {
            if (isInterpolated) GifType.INTERPOLATED else GifType.DEFAULT
        }
        initSampleAnimation(tempType)
    }

    private fun initSampleAnimation(type: GifType) =
        GifDrawable(requireContext().assets, type.path).apply {
            sampleGifDrawable = this

            val rev = if (binding.reverse.isChecked) 0.5f else 1f
            setSpeed((duration / (binding.durationSlider.value * durationStep * 1000)) * rev)

            binding.sampleGif.setImageDrawable(this)
        }

    private enum class GifType(val path: String) {
        DEFAULT("sample.gif"),
        DEFAULT_REVERSE("sample_reverse.gif"),
        INTERPOLATED("sample_interpolated.gif"),
        INTERPOLATED_REVERSE("sample_interpolated_reverse.gif")
    }
}
