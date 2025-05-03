package ru.ptrff.photopano.loading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.ptrff.photopano.R
import ru.ptrff.photopano.databinding.FragmentLoadingBinding
import ru.ptrff.photopano.loading.presentation.LoadingSideEffects
import ru.ptrff.photopano.loading.presentation.LoadingSideEffects.ChangeAnimationDescription
import ru.ptrff.photopano.loading.presentation.LoadingSideEffects.ProcessingDone
import ru.ptrff.photopano.loading.presentation.LoadingStore
import ru.ptrff.photopano.loading.presentation.LoadingUiEvents.ProcessArguments
import ru.ptrff.photopano.loading.presentation.LoadingUiEvents.StartProcessing
import ru.ptrff.photopano.utils.initObservers
import ru.ptrff.photopano.utils.viewBinding

@AndroidEntryPoint
class LoadingFragment : Fragment() {

    private val binding by viewBinding(FragmentLoadingBinding::inflate)
    private val store by viewModels<LoadingStore>()

    private var animationRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObservers(
            store,
            initUiEvents = listOf(
                ProcessArguments(arguments),
                StartProcessing(requireContext().filesDir)
            ),
            onStateChanged = { },
            onSideEffect = ::handleSideEffect
        )
    }

    private fun handleSideEffect(sideEffect: LoadingSideEffects) = when (sideEffect) {
        is ChangeAnimationDescription -> with(binding.loadingDescription) {
            post { setText(sideEffect.stringRes) }
        }

        is ProcessingDone -> processingDone(sideEffect.upload)
    }

    private fun processingDone(upload: Boolean) {
        val args = Bundle()
        args.putBoolean("upload", upload)
        binding.root.findNavController()
            .navigate(R.id.action_loadingFragment_to_resultFragment, args)
    }

    private fun startAnimation() = with(binding) {
        animationRunning = true

        loadingLabel.post {
            loadingLabel.layoutParams.width = loadingLabel.measuredWidth
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
