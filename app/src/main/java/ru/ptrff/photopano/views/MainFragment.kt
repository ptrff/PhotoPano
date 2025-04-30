package ru.ptrff.photopano.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
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
            findNavController(it).navigate(R.id.action_mainFragment_to_parametersFragment)
        }
        binding.settings.setOnClickListener {
            findNavController(it).navigate(R.id.action_mainFragment_to_settingsFragment)
        }
    }

    private fun animateBackground() {
        backgroundAnimationRunning = true

        // Dotted circles
        animateS1()
        animateS2()
        animateS3()

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

    private fun animateMotivationText() {
        if (!backgroundAnimationRunning) return

        if (binding.motivationText.text.toString().endsWith("|")) {
            binding.motivationText.setText(R.string.motivation_text)
        } else {
            binding.motivationText.append(" |")
        }

        binding.motivationText.animate()
            .withEndAction { this.animateMotivationText() }
            .setDuration(600)
            .start()
    }

    private fun animateS3() {
        if (!backgroundAnimationRunning) return

        binding.circleS3.animate()
            .rotationBy(360f)
            .setDuration(100000)
            .setInterpolator(LinearInterpolator())
            .withEndAction { this.animateS3() }
            .start()
    }

    private fun animateS2() {
        if (!backgroundAnimationRunning) return

        binding.circleS2.animate()
            .rotationBy(360f)
            .setDuration(180000)
            .setInterpolator(LinearInterpolator())
            .withEndAction { this.animateS2() }
            .start()
    }

    private fun animateS1() {
        if (!backgroundAnimationRunning) return

        binding.circleS1.animate()
            .rotationBy(360f)
            .setDuration(300000)
            .setInterpolator(LinearInterpolator())
            .withEndAction { this.animateS1() }
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
