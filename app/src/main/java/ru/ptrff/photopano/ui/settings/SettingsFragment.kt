package ru.ptrff.photopano.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import dagger.hilt.android.AndroidEntryPoint
import ru.ptrff.photopano.databinding.FragmentSettingsBinding
import ru.ptrff.photopano.ui.settings.SettingsSideEffects.SpanCountChanged
import ru.ptrff.photopano.ui.settings.SettingsUiEvents.DecreasePackCount
import ru.ptrff.photopano.ui.settings.SettingsUiEvents.IncreasePackCount
import ru.ptrff.photopano.ui.settings.SettingsUiEvents.Initialize
import ru.ptrff.photopano.ui.settings.SettingsUiEvents.SaveSequence
import ru.ptrff.photopano.ui.settings.adapters.DragNDropCallback
import ru.ptrff.photopano.ui.settings.adapters.SettingsAdapter
import ru.ptrff.photopano.ui.settings.adapters.SettingsLayoutManager
import ru.ptrff.photopano.utils.fastLazy
import ru.ptrff.photopano.utils.initObservers
import ru.ptrff.photopano.utils.viewBinding

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val binding by viewBinding(FragmentSettingsBinding::inflate)
    private val viewModel by viewModels<SettingsViewModel>()
    private val adapter: SettingsAdapter by fastLazy {
        SettingsAdapter(layoutInflater, viewModel.cameraUtils)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClicks()

        initObservers(
            viewModel,
            onStateChanged = ::render,
            onSideEffect = ::handleSideEffects
        ).also {
            viewModel.onEvent(Initialize(resources.displayMetrics))
        }
    }

    private fun render(state: SettingsState) = with(state) {
        binding.packCount.text = packCount.toString()
    }

    private fun handleSideEffects(sideEffect: SettingsSideEffects) = when (sideEffect) {
        is SpanCountChanged -> initRecycler(sideEffect.spanCount)
    }

    private fun initRecycler(spanCount: Int) = with(binding) {
        settings.layoutManager = SettingsLayoutManager(requireContext(), spanCount, 1)
        settings.adapter = adapter
        ItemTouchHelper(
            DragNDropCallback(
                preferenceCount = 1,
                adapterCallback = adapter::onItemMove
            )
        ).attachToRecyclerView(settings)
    }

    private fun initClicks() = with(binding) {
        back.setOnClickListener {
            adapter.stopPreviewing()
            it.findNavController().popBackStack()
        }
        updatePreviews.setOnClickListener { adapter.reshoot() }
        saveSequence.setOnClickListener {
            viewModel.onEvent(SaveSequence)
        }
        packsLess.setOnClickListener {
            viewModel.onEvent(DecreasePackCount)
        }
        packsMore.setOnClickListener {
            viewModel.onEvent(IncreasePackCount)
        }
    }
}
