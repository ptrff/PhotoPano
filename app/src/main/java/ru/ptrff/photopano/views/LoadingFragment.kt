package ru.ptrff.photopano.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import ru.ptrff.photopano.R
import ru.ptrff.photopano.databinding.FragmentLoadingBinding
import ru.ptrff.photopano.utils.AnimationUtils
import ru.ptrff.photopano.utils.CameraUtils
import ru.ptrff.photopano.utils.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class LoadingFragment : Fragment() {

    private val binding by viewBinding(FragmentLoadingBinding::inflate)

    private var animationRunning = false
    private var duration = 0f
    private var interpolate = false
    private var reverse = false
    private var upload = false

    @Inject
    lateinit var cameraUtils: CameraUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root.also {
        arguments?.let {
            duration = it.getFloat("duration", 1f)
            interpolate = it.getBoolean("interpolate", false)
            reverse = it.getBoolean("reverse", false)
            upload = it.getBoolean("upload", false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startProcessing()
    }

    @SuppressLint("CheckResult")
    private fun startProcessing() = AnimationUtils(requireContext(), duration).apply {

        setCameras(cameraUtils.supportedCameras)
        changeAnimationDescription =
            { binding.root.post { binding.loadingDescription.setText(it) } }

        prepareEnvironment()
            .andThen(Completable.defer(::combineImages))
            .andThen(Completable.defer(::createPalette))
            .andThen(Completable.defer {
                if (interpolate) {
                    interpolation()
                } else {
                    Completable.complete()
                }
            })
            .andThen(Completable.defer {
                if (reverse) {
                    reverseAnimation()
                } else {
                    convertToGif()
                }
            })
            .andThen(Completable.defer(::emptyTemp))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::processingDone) { throwable: Throwable ->
                Log.e(
                    MainActivity.TAG,
                    "error creating animation: " + throwable.message
                )
            }
    }

    private fun processingDone() {
        val args = Bundle()
        args.putBoolean("upload", upload)
        findNavController(binding.root)
            .navigate(R.id.action_loadingFragment_to_resultFragment, args)
    }

    private fun startAnimation() {
        animationRunning = true

        binding.loadingLabel.post {
            binding.loadingLabel.layoutParams.width = binding.loadingLabel.measuredWidth
            dotAnimation()
        }
    }

    private fun dotAnimation() {
        if (binding.loadingLabel.text.toString().endsWith("...")) {
            binding.loadingLabel.text = binding.loadingLabel.text.toString()
                .substring(0, binding.loadingLabel.text.toString().length - 3)
        } else {
            binding.loadingLabel.append(".")
        }

        binding.loadingLabel.animate().setDuration(600).withEndAction {
            if (animationRunning) {
                dotAnimation()
            }
        }.start()
    }

    private fun stopAnimation() {
        animationRunning = false
    }

    override fun onResume() {
        super.onResume()
        startAnimation()
    }

    override fun onPause() {
        super.onPause()
        stopAnimation()
    }
}
