package ru.ptrff.photopano.result

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogo
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import kotlinx.coroutines.launch
import ru.ptrff.photopano.R
import ru.ptrff.photopano.databinding.FragmentResultBinding
import ru.ptrff.photopano.MainActivity
import ru.ptrff.photopano.result.presentation.ResultSideEffects
import ru.ptrff.photopano.result.presentation.ResultState
import ru.ptrff.photopano.result.presentation.ResultUiEvents
import ru.ptrff.photopano.result.presentation.ResultStore
import ru.ptrff.photopano.utils.fastLazy
import ru.ptrff.photopano.utils.viewBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.min
import com.google.android.material.R as MaterialR

class ResultFragment : Fragment() {
    private val binding by viewBinding(FragmentResultBinding::inflate)
    private val viewModel by viewModels<ResultStore>()
    private val upload by fastLazy {
        requireArguments().getBoolean("upload", false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initClicks()
        animateCompliments()
        initObservers()

        File(requireContext().filesDir, "output.gif").takeIf { it.exists() }?.let { file ->
            uploadGif(file)
            loadGif(file)
        } ?: run { showError() }
    }

    private fun uploadGif(file: File) {
        if (upload) {
            viewModel.onEvent(ResultUiEvents.UploadGif(file))
        } else {
            binding.uploading.setText(R.string.upload_to_cloud)
            binding.uploading.setOnClickListener { v: View? ->
                binding.uploading.setText(R.string.uploading)
                viewModel.onEvent(ResultUiEvents.UploadGif(file))
            }
        }
    }

    private fun initObservers() = with(viewModel) {
        viewModelScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { state.collect { render(it) } }
                launch { sideEffect.collect { handleSideEffect(it) } }
            }
        }
    }

    private fun render(state: ResultState) = with(state) {
        when (progress) {
            -1 -> updateProgress(0)
            100 -> {
                updateProgress(progress)
                animateDoneUploading()
            }

            else -> updateProgress(progress)
        }
    }

    private fun handleSideEffect(effect: ResultSideEffects) = when (effect) {
        is ResultSideEffects.GenerateAndPasteQR -> generateAndPasteQR(effect.url)
    }

    private fun generateAndPasteQR(url: String) {
        Log.d("MYLOG", "HIHIHHIH $url")
        val qrData: QrData = QrData.Url(url)
        val options: QrVectorOptions = QrVectorOptions.Builder()
            .setLogo(
                QrVectorLogo(
                    AppCompatResources.getDrawable(requireContext(), R.drawable.light_rtuitlab),
                    0.1f,
                    QrVectorLogoPadding.Natural(0.2f),
                    QrVectorLogoShape.RoundCorners(0.5f),
                    { drawable, i, i1 ->
                        (drawable as BitmapDrawable).bitmap.scale(i, i1, false)
                    },
                    QrVectorColor.Solid(Color.TRANSPARENT)
                )
            )
            .setColors(
                QrVectorColors(
                    QrVectorColor.Solid("#E3E1E9".toColorInt()),
                    QrVectorColor.Solid("#00FFFFFF".toColorInt()),
                    QrVectorColor.Solid("#E3E1E9".toColorInt()),
                    QrVectorColor.Solid("#E3E1E9".toColorInt())
                )
            )
            .build()

        binding.qrCode.setImageDrawable(QrCodeDrawable(qrData, options, null))
    }

    private fun updateProgress(progress: Int) {
        val oldLevel = binding.uploading.background.level
        val progressLevel = min((progress * 100).toDouble(), 10000.0).toInt()

        ValueAnimator.ofInt(oldLevel, progressLevel).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                binding.uploading.background.setLevel(it.animatedValue as Int)
            }
            start()
        }
    }

    private fun animateDoneUploading() {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(
            MaterialR.attr.colorOnTertiary,
            typedValue,
            true
        )
        val colorFrom = typedValue.data
        requireContext().theme.resolveAttribute(
            MaterialR.attr.colorTertiaryContainer,
            typedValue,
            true
        )
        val colorTo = typedValue.data

        ObjectAnimator.ofArgb(
            binding.uploading.background,
            "tint",
            colorFrom,
            colorTo
        ).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            start()
        }

        binding.uploading.text = getString(R.string.download)

        initDownloadClicks()
    }

    private fun loadGif(file: File) = Glide.with(this)
        .asGif()
        .load(file)
        .addListener(object : RequestListener<GifDrawable?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<GifDrawable?>,
                isFirstResource: Boolean
            ): Boolean {
                showError()
                return true
            }

            override fun onResourceReady(
                resource: GifDrawable,
                model: Any,
                target: Target<GifDrawable?>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                animateImageAppearance()
                return false
            }
        })
        .skipMemoryCache(true)
        .into(binding.resultImage)

    private fun showError() = binding.title.setText(R.string.done_error)

    private fun animateImageAppearance() = binding.resultRoot.animate()
        .scaleY(1f)
        .setInterpolator(DecelerateInterpolator())
        .setDuration(500)
        .start()

    private fun animateCompliments() = with(binding) {
        complimentsText.post {
            val height = complimentsText.layout.height
            complimentsText.layoutParams.height = height * 2
            complimentsText.requestLayout()
            this@ResultFragment.doubleCompliments()

            val halfLineSpacingInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                5f,
                resources.displayMetrics
            ).toInt()

            TranslateAnimation(
                0f,
                0f,
                0f,
                (-height - halfLineSpacingInPx).toFloat()
            ).apply {
                duration = height * 40L
                interpolator = LinearInterpolator()
                repeatCount = Animation.INFINITE
                repeatMode = Animation.RESTART
                complimentsText.startAnimation(this)
            }
        }
    }

    private fun stopComplimentsAnimation() {
        binding.complimentsText.clearAnimation()
    }

    private fun doubleCompliments() {
        binding.complimentsText.append(
            """
                
                ${binding.complimentsText.text}
            """.trimIndent()
        )
    }

    private fun initDownloadClicks() = with(binding) {
        uploading.setOnClickListener {
            uploading.isEnabled = false
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(
                MaterialR.attr.colorOnTertiary,
                typedValue,
                true
            )
            uploading.backgroundTintList = ColorStateList.valueOf(typedValue.data)

            animateFadeTransition(complimentsRoot, qrCodeRoot)
            stopComplimentsAnimation()
        }

        email.setOnClickListener { animateFadeTransition(qrCodeRoot, enterMailRoot) }
        back.setOnClickListener { animateFadeTransition(enterMailRoot, qrCodeRoot) }
    }

    private fun animateFadeTransition(from: View, to: View) {
        to.visibility = View.VISIBLE
        from.animate()
            .alpha(0f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(500)
            .withEndAction { from.visibility = View.GONE }
            .start()
        to.animate()
            .alpha(1f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(500)
            .start()
    }

    private val storageActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(
                    requireContext(),
                    R.string.need_storage_permission,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", requireActivity().packageName, null)
            intent.setData(uri)
            storageActivityResultLauncher.launch(intent)
        }
    }

    private fun saveToExternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestStoragePermission()
                return
            }
        } else {
            val write = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val read = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            if (read != PackageManager.PERMISSION_GRANTED || write != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission()
                return
            }
        }

        if (!checkForEmail()) return

        val mail = binding.emailField.text.toString().lowercase(Locale.getDefault()).trim()

        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
        val from = File(requireContext().filesDir, "output.gif")
        if (from.exists()) {
            val to = File(path, "$mail.gif")
            try {
                val outChannel = FileOutputStream(to).channel
                val inChannel = FileInputStream(from).channel
                inChannel.transferTo(0, inChannel.size(), outChannel)
                Toast.makeText(requireContext(), "done", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.message?.let { Log.d(MainActivity.Companion.TAG, it) }
            }
        }
    }

    private fun checkForEmail(): Boolean {
        val mail = binding.emailField.text.toString()
        if (mail.isEmpty()) {
            Toast.makeText(requireContext(), R.string.enter_mail, Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            Toast.makeText(requireContext(), R.string.wrong_mail, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun initClicks() {
        binding.home.setOnClickListener {
            it.findNavController().navigate(R.id.action_resultFragment_to_mainFragment)
        }

        binding.reshoot.setOnClickListener {
            it.findNavController().navigate(R.id.action_resultFragment_to_parametersFragment)
        }

        binding.done.setOnClickListener { saveToExternal() }
    }
}
