package ru.ptrff.photopano.ui.settings.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ru.ptrff.photopano.R
import ru.ptrff.photopano.databinding.ItemSettingCameraBinding
import ru.ptrff.photopano.databinding.ItemSettingClickableLabelBinding
import ru.ptrff.photopano.databinding.ItemSettingLabelBinding
import ru.ptrff.photopano.models.Camera
import ru.ptrff.photopano.ui.MainActivity.Companion.TAG
import ru.ptrff.photopano.ui.settings.adapters.SettingsAdapter.SettingsItem.CameraHolder
import ru.ptrff.photopano.ui.settings.adapters.SettingsAdapter.SettingsItem.ClickableLabelHolder
import ru.ptrff.photopano.ui.settings.adapters.SettingsAdapter.SettingsItem.LabelHolder
import ru.ptrff.photopano.ui.settings.adapters.SettingsAdapter.ViewType.entries
import ru.ptrff.photopano.utils.CameraUtils
import java.util.Collections

class SettingsAdapter(
    private val inflater: LayoutInflater,
    private val cameraUtils: CameraUtils
) : RecyclerView.Adapter<SettingsAdapter.SettingsItem>() {

    private var previewQueue: BehaviorSubject<Completable> = BehaviorSubject.create<Completable>()
    private var reIniting = false

    init {
        initPreviewQueue()
    }

    @SuppressLint("CheckResult")
    private fun initPreviewQueue() {
        previewQueue = BehaviorSubject.create<Completable>()

        previewQueue
            .observeOn(Schedulers.io())
            .concatMap(Completable::toObservable)
            .subscribeBy(onError = { Log.e(TAG, it.message ?: "") })
    }

    override fun onBindViewHolder(
        holder: SettingsItem,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        when (payloads[0] as BindAction) {
            BindAction.RESHOOT -> if (holder is CameraHolder) {
                val camera = cameraUtils.cameraList[position - 1]
                updatePreview(camera, holder.binding.preview)
            }
        }
    }

    override fun onBindViewHolder(
        holder: SettingsItem,
        position: Int
    ) = when (ViewType.fromInt(getItemViewType(position))) {
        ViewType.CLICKABLE_LABEL -> setupClickableLabels(holder as ClickableLabelHolder)
        ViewType.CAMERA -> setupCamera(holder as CameraHolder, position)
        else -> Unit
    }

    private fun setupClickableLabels(
        holder: ClickableLabelHolder
    ): Unit = with(holder) {
        binding.label.setText(R.string.find_cameras)
        binding.description.setText(R.string.click_to_rescan)
        binding.clickableRoot.setOnClickListener {
            if (!reIniting) {
                reInit(binding.getRoot().context)
            }
        }
    }

    private fun setupCamera(
        holder: CameraHolder,
        position: Int
    ): Unit = with(holder) {
        val camera = cameraUtils.cameraList[position - 1]

        binding.num.text = camera.id
        binding.pack.text = camera.packId.toString()
        binding.preview.post {
            updatePreview(camera, binding.preview)
        }
    }

    fun reshoot() = notifyItemRangeChanged(1, this.itemCount - 1, BindAction.RESHOOT)

    private fun updatePreview(
        camera: Camera,
        preview: TextureView
    ) = previewQueue.onNext(
        Completable.create { emitter ->
            cameraUtils.openPreview(
                camera,
                preview,
                onOpenedCallback = {
                    cameraUtils.startPreview(camera)
                },
                onFrameAcquiredCallback = {
                    transformTextureView(preview)
                    // TODO change angle by tapping on camera
                    cameraUtils.close(camera)
                },
                onClosedCallback = emitter::onComplete
            )
        }
    )

    fun transformTextureView(textureView: TextureView, angle: Int = 0) = textureView.apply {
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()
        setTransform(Matrix().apply {
            postRotate(270F + angle, width / 2f, height / 2f);
            postScale(width / height, height / width, width / 2f, height / 2f);
        });
    }

    fun stopPreviewing() {
        previewQueue.onComplete()
        cameraUtils.closeAll()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < toPosition) {
            for (i in fromPosition..<toPosition) {
                val realIndex = i - 1 // because of label before

                Collections.swap(
                    cameraUtils.cameraList,
                    realIndex,
                    realIndex + 1
                )
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                val realIndex = i - 1 // because of label before

                Collections.swap(
                    cameraUtils.cameraList,
                    realIndex,
                    realIndex - 1
                )
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    private fun reInit(context: Context) {
        reIniting = true

        val countBefore = cameraUtils.cameraCount
        stopPreviewing()
        cameraUtils.reInit(context)
        val countAfter = cameraUtils.cameraCount
        if (countBefore > countAfter) {
            notifyItemRangeRemoved(countAfter, countBefore)
            notifyItemRangeChanged(1, countAfter)
        } else {
            notifyItemRangeInserted(countBefore, countAfter - countBefore)
            notifyItemRangeChanged(1, countAfter)
        }
        initPreviewQueue()

        reIniting = false
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        intViewType: Int
    ): SettingsItem = when (ViewType.fromInt(intViewType)) {
        ViewType.LABEL -> LabelHolder(
            ItemSettingLabelBinding.inflate(inflater, parent, false)
        )

        ViewType.CLICKABLE_LABEL -> ClickableLabelHolder(
            ItemSettingClickableLabelBinding.inflate(inflater, parent, false)
        )

        ViewType.CAMERA -> CameraHolder(
            ItemSettingCameraBinding.inflate(inflater, parent, false)
        )
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return ViewType.LABEL.int
        if (position == itemCount - 1) return ViewType.CLICKABLE_LABEL.int

        return ViewType.CAMERA.int
    }

    // +1 - for bottom settings +1 - for label
    override fun getItemCount(): Int = cameraUtils.cameraCount + 2

    enum class BindAction {
        RESHOOT
    }

    enum class ViewType(val int: Int) {
        LABEL(0),
        CAMERA(1),
        CLICKABLE_LABEL(3);

        companion object {
            fun fromInt(i: Int): ViewType = entries.first { it.int == i }
        }
    }

    sealed class SettingsItem(
        open val binding: ViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        class CameraHolder(
            override val binding: ItemSettingCameraBinding
        ) : SettingsItem(binding)

        class ClickableLabelHolder(
            override val binding: ItemSettingClickableLabelBinding
        ) : SettingsItem(binding)

        class LabelHolder(
            override val binding: ItemSettingLabelBinding
        ) : SettingsItem(binding)
    }
}
