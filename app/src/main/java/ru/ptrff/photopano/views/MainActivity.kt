package ru.ptrff.photopano.views

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.ptrff.photopano.R
import ru.ptrff.photopano.databinding.ActivityMainBinding
import ru.ptrff.photopano.utils.CameraUtils
import ru.ptrff.photopano.utils.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::inflate)

    @Inject
    lateinit var cameraUtils: CameraUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCameraPermission()

        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        enableFullScreen()
    }

    private val cameraPermissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraUtils.init(this)
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.well_errors),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun requestCameraPermission() {
        cameraPermissionRequestLauncher.launch("android.permission.CAMERA")
    }

    @Suppress("Deprecation")
    private fun enableFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.hide(WindowInsets.Type.systemBars())
            }
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    companion object {
        const val TAG: String = "PPDebug"
    }
}