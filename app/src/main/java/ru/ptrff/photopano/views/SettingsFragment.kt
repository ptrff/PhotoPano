package ru.ptrff.photopano.views

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import dagger.hilt.android.AndroidEntryPoint
import ru.ptrff.photopano.adapters.DragNDropCallback
import ru.ptrff.photopano.adapters.SettingsAdapter
import ru.ptrff.photopano.adapters.SettingsLayoutManager
import ru.ptrff.photopano.databinding.FragmentSettingsBinding
import ru.ptrff.photopano.utils.CameraUtils
import ru.ptrff.photopano.utils.fastLazy
import ru.ptrff.photopano.utils.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val binding by viewBinding(FragmentSettingsBinding::inflate)
    private val adapter by fastLazy {
        SettingsAdapter(layoutInflater, cameraUtils)
    }
    private var packCount = 1
    private val prefs: SharedPreferences by fastLazy {
        requireContext().getSharedPreferences("cameraprefs", Context.MODE_PRIVATE)
    }

    @Inject
    lateinit var cameraUtils: CameraUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecycler()
        loadData()
        initClicks()
    }

    private fun initRecycler() = with(binding) {
        settings.layoutManager = SettingsLayoutManager(requireContext(), spanCount, 1)
        settings.adapter = adapter
        ItemTouchHelper(
            DragNDropCallback(
                preferenceCount = 1,
                adapterCallback = adapter::onItemMove
            )
        ).attachToRecyclerView(settings)
    }

    private fun loadData() {
        if (prefs.contains("packCount")) {
            packCount = prefs.getInt("packCount", 1)
            binding.packCount.text = packCount.toString()
        }
    }

    private val spanCount: Int
        get() {
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHorizontalPadding = 32 * resources.displayMetrics.densityDpi / 160
            val itemWidth = 140 * resources.displayMetrics.densityDpi / 160
            val gap = 16 * resources.displayMetrics.densityDpi / 160

            return (screenWidth - screenHorizontalPadding) / (itemWidth + gap)
        }

    private fun initClicks() = with(binding) {
        back.setOnClickListener { v: View ->
            adapter.stopPreviewing()
            findNavController(v).popBackStack()
        }
        updatePreviews.setOnClickListener { adapter.reshoot() }
        saveSequence.setOnClickListener {
            cameraUtils.saveCameraList(this@SettingsFragment.packCount)
        }
        packsLess.setOnClickListener { decreasePackCount() }
        packsMore.setOnClickListener { increasePackCount() }
    }

    private fun increasePackCount() {
        if (packCount >= 9) return
        packCount++
        binding.packCount.text = packCount.toString()
    }

    private fun decreasePackCount() {
        if (packCount <= 1) return
        packCount--
        binding.packCount.text = packCount.toString()
    }
}
