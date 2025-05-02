package ru.ptrff.photopano.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import ru.ptrff.photopano.R
import ru.ptrff.photopano.databinding.FragmentMainBinding
import ru.ptrff.photopano.utils.viewBinding

class MainFragment : Fragment() {

    private val binding by viewBinding(FragmentMainBinding::inflate)
    private var backgroundAnimationRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClicks()
    }

    private fun initClicks() {
        binding.start.setOnClickListener {
            it.findNavController().navigate(R.id.action_mainFragment_to_parametersFragment)
        }
        binding.settings.setOnClickListener {
            it.findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }
    }

    private fun animateBackground() {
        backgroundAnimationRunning = true

        // Dotted circles
        animateCircle(binding.circleS1, 300000)
        animateCircle(binding.circleS2, 180000)
        animateCircle(binding.circleS3, 100000)

        // Motivation text
        animateMotivationText()
    }

    private fun stopBackgroundAnimations() = with(binding) {
        backgroundAnimationRunning = false
        circleS1.clearAnimation()
        circleS2.clearAnimation()
        circleS3.clearAnimation()
        motivationText.clearAnimation()
    }

    private fun animateMotivationText() = with(binding.motivationText) {
        if (!backgroundAnimationRunning) return

        if (text.endsWith("|")) {
            setText(R.string.motivation_text)
        } else {
            append(" |")
        }

        animate()
            .withEndAction(::animateMotivationText)
            .setDuration(600)
            .start()
    }

    private fun animateCircle(circle: View, duration: Long) {
        if (!backgroundAnimationRunning) return

        circle.animate()
            .rotationBy(360f)
            .setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .withEndAction { animateCircle(circle, duration) }
            .start()
    }

    override fun onPause() {
        super.onPause()
        stopBackgroundAnimations()
    }

    override fun onStart() {
        super.onStart()
        animateBackground()
    }
}
