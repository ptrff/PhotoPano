package ru.ptrff.photopano.settings

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
import ru.ptrff.photopano.settings.adapters.DragNDropCallback
import ru.ptrff.photopano.settings.adapters.SettingsAdapter
import ru.ptrff.photopano.settings.adapters.SettingsLayoutManager
import ru.ptrff.photopano.settings.presentation.SettingsSideEffects
import ru.ptrff.photopano.settings.presentation.SettingsSideEffects.SpanCountChanged
import ru.ptrff.photopano.settings.presentation.SettingsState
import ru.ptrff.photopano.settings.presentation.SettingsStore
import ru.ptrff.photopano.settings.presentation.SettingsUiEvents.DecreasePackCount
import ru.ptrff.photopano.settings.presentation.SettingsUiEvents.IncreasePackCount
import ru.ptrff.photopano.settings.presentation.SettingsUiEvents.Initialize
import ru.ptrff.photopano.settings.presentation.SettingsUiEvents.SaveSequence
import ru.ptrff.photopano.utils.fastLazy
import ru.ptrff.photopano.utils.initObservers
import ru.ptrff.photopano.utils.viewBinding

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val binding by viewBinding(FragmentSettingsBinding::inflate)
    private val store by viewModels<SettingsStore>()
    private val adapter: SettingsAdapter by fastLazy {
        SettingsAdapter(layoutInflater, store.cameraUtils)
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
            store,
            initUiEvents = listOf(Initialize(resources.displayMetrics)),
            onStateChanged = ::render,
            onSideEffect = ::handleSideEffects
        )
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
            store.onEvent(SaveSequence)
        }
        packsLess.setOnClickListener {
            store.onEvent(DecreasePackCount)
        }
        packsMore.setOnClickListener {
            store.onEvent(IncreasePackCount)
        }
    }
}
