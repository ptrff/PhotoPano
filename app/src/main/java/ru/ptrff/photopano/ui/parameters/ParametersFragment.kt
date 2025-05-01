package ru.ptrff.photopano.ui.parameters

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.droidsonroids.gif.GifDrawable
import ru.ptrff.photopano.R
import ru.ptrff.photopano.databinding.FragmentParametersBinding
import ru.ptrff.photopano.ui.parameters.ParametersSideEffects.*
import ru.ptrff.photopano.ui.parameters.ParametersUiEvents.*
import ru.ptrff.photopano.utils.initObservers
import ru.ptrff.photopano.utils.viewBinding
import java.util.Locale
import com.google.android.material.R as MaterialR

@AndroidEntryPoint
class ParametersFragment : Fragment() {
    private val binding by viewBinding(FragmentParametersBinding::inflate)
    private lateinit var counterDialog: CounterDialog

    private val viewModel by viewModels<ParametersViewModel>()

    private val minDuration: Float = 0.5f
    private val maxDuration: Float = 5f
    private val durationStep: Float = 0.5f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClicks()
        initSliders()

        initObservers(
            viewModel,
            onStateChanged = ::render,
            onSideEffect = ::handleSideEffects
        ).also {
            viewModel.onEvent(Initialize)
        }
    }

    private fun initClicks() = with(binding) {
        back.setOnClickListener {
            it.findNavController().popBackStack()
        }

        durationSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.onEvent(OnShootingDurationChange(value))
            }
        }

        preparationSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.onEvent(OnPrepareDurationChange(value.toInt()))
            }
        }

        done.setOnClickListener {
            viewModel.onEvent(OnDoneClicked)
        }

        reverse.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEvent(OnReverseChange(isChecked))
        }

        interpolate.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEvent(OnInterpolateChange(isChecked))
        }
    }

    private fun showDialog(prepareTime: Int, cameraCount: Int) {
        counterDialog = CounterDialog(prepareTime, cameraCount, layoutInflater).apply {
            setOnDismissListener {
                changeDoneButtonState(enabled = true)
                viewModel.onEvent(OnCounterDialogDismiss)
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

                startFlashes(cameraCount)
                viewModel.onEvent(OnCounterDialogFinish)
            }

            show()
        }
    }

    private fun shootingComplete() {
        counterDialog.counterComplete()
        Bundle().apply {
            putFloat("duration", binding.durationSlider.value)
            putBoolean("interpolate", binding.interpolate.isChecked)
            putBoolean("reverse", binding.reverse.isChecked)
            putBoolean("upload", binding.upload.isChecked)
            binding.back.findNavController()
                .navigate(R.id.action_global_loadingFragment, this)
        }
    }

    private fun initSliders() = with(binding) {
        durationSlider.valueFrom = minDuration
        durationSlider.valueTo = maxDuration
        durationSlider.stepSize = durationStep
    }

    private fun render(state: ParametersState) = with(state) {
        binding.durationSlider.value = state.shootingDuration
        binding.durationValue.text = String.format(Locale.US, "%.2f", state.shootingDuration)

        binding.preparationSlider.value = state.prepareDuration.toFloat()
        binding.preparationValue.text = state.prepareDuration.toString()

        binding.interpolate.isChecked = interpolate
        binding.reverse.isChecked = reverse
        binding.upload.isChecked = upload

        changeGifAnimation(gifType, state)
    }

    private fun handleSideEffects(sideEffect: ParametersSideEffects) = when (sideEffect) {
        is ShowCounterDialog -> {
            changeDoneButtonState(enabled = false)
            showDialog(sideEffect.prepareTime, sideEffect.cameraCount)
        }

        is ShootingComplete -> shootingComplete()
        is NextShootingStep -> counterDialog.nextStep()
    }

    private fun changeGifAnimation(type: GifType, state: ParametersState) = GifDrawable(
        requireContext().assets,
        type.path
    ).apply {
        val rev = if (state.reverse) 0.5f else 1f
        setSpeed((duration / (state.shootingDuration * durationStep * 1000)) * rev)

        binding.sampleGif.setImageDrawable(this)
    }

    private fun changeDoneButtonState(enabled: Boolean) {
        binding.done.isEnabled = enabled
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(
            if (enabled) MaterialR.attr.colorTertiaryContainer else MaterialR.attr.colorOnTertiary,
            typedValue,
            true
        )
        binding.done.setBackgroundColor(typedValue.data)
    }

    private fun startFlashes(cameraCount: Int) {
        val drawable = with(requireContext()) {
            RotatedGradientDrawable(
                context = this,
                colors = intArrayOf(
                    getColor(android.R.color.white),
                    getColor(R.color.white_alpha_02),
                    getColor(R.color.md_theme_background_transparent)
                ),
                positions = floatArrayOf(0f, 0.6f, 1f),
                width = binding.flashes.measuredWidth,
                height = binding.flashes.measuredHeight
            )
        }
        binding.flashes.background = drawable

        ObjectAnimator.ofFloat(0f, 360f).apply {
            setDuration(3600L * cameraCount)
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { drawable.setRotationDegrees(it.animatedValue as Float) }
            start()
        }
    }
}
