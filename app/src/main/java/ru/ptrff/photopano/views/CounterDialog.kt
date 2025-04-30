package ru.ptrff.photopano.views

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.DialogInterface.OnDismissListener
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout.LayoutParams
import androidx.fragment.app.DialogFragment
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ptrff.photopano.databinding.FragmentCounterBinding
import ru.ptrff.photopano.utils.fastLazy
import ru.ptrff.photopano.utils.viewBinding

class CounterDialog(
    private var duration: Int,
    private val cameraCount: Int
) : DialogFragment() {
    private val binding by viewBinding(FragmentCounterBinding::inflate)
    private val dialog by fastLazy { createDialog(layoutInflater) }
    private val density by fastLazy {
        layoutInflater.context.resources.displayMetrics.density
    }

    lateinit var startShootingCallback: () -> Unit
    lateinit var fadeOutParametersCallback: (duration: Int) -> Unit

    var scaleStep = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDialog()
    }

    private fun createDialog(inflater: LayoutInflater) =
        MaterialAlertDialogBuilder(inflater.context)
            .setView(binding.getRoot())
            .create()

    fun setOnDismissListener(onDismissListener: OnDismissListener) {
        dialog.setOnDismissListener(onDismissListener)
    }

    private fun setupDialog() = with(dialog) {
        setCanceledOnTouchOutside(false)

        window?.setLayout(
            (360 * density).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            R.attr.colorOnSecondary,
            typedValue,
            true
        )
        window?.decorView?.backgroundTintList = ColorStateList.valueOf(typedValue.data)

        // prepare views
        binding.duration.text = duration.toString()
        binding.shootingProgress.max = cameraCount
        binding.cancel.setOnClickListener { dismiss() }
        binding.start.setOnClickListener {
            fadeOutParametersCallback(duration * 1000)
            transformAndStartCounter()
        }
    }

    private fun transformAndStartCounter() = with(binding) {
        root.layoutParams.height = root.measuredHeight

        TransitionSet().apply {
            addTransition(ChangeBounds())
            setDuration(500)
            setInterpolator(DecelerateInterpolator())
            TransitionManager.beginDelayedTransition(root, this)
        }

        root.removeView(buttonPanel)
        root.removeView(topText)
        secondsTextLayout.removeView(secondsText)
        root.removeView(bottomText)
        secondsTextLayout.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

        duration.animate()
            .scaleY(1.5f)
            .scaleX(1.5f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { this@CounterDialog.count() }
            .start()

        scaleStep = 3f / this@CounterDialog.duration

        dialog.setCancelable(false)
    }

    private fun count() {
        val typedValue = TypedValue()
        binding.duration.context.theme.resolveAttribute(
            R.attr.colorError,
            typedValue,
            true
        )

        if (duration <= 4 && binding.duration.currentTextColor != typedValue.data) {
            // change color to red
            val colorFrom = binding.duration.currentTextColor
            val colorTo = typedValue.data
            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
            colorAnimation.setDuration(1000)
            colorAnimation.interpolator = DecelerateInterpolator()
            colorAnimation.addUpdateListener { animator: ValueAnimator ->
                binding.duration.setTextColor(
                    animator.animatedValue as Int
                )
            }
            colorAnimation.start()
        }
        if (duration != 1) {
            // scale and decrease
            duration--
            binding.duration.text = duration.toString()
            binding.duration.animate()
                .scaleY(binding.duration.scaleX + scaleStep)
                .scaleX(binding.duration.scaleX + scaleStep)
                .setDuration(1000)
                .withEndAction { this.count() }
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            transformAndStartShooting()
        }
    }

    private fun transformAndStartShooting() {
        TransitionSet().apply {
            addTransition(ChangeBounds())
            setDuration(500)
            setInterpolator(DecelerateInterpolator())
            TransitionManager.beginDelayedTransition(binding.root, this)
        }
        binding.secondsTextLayout.layoutParams = LayoutParams(MATCH_PARENT, 0)
        binding.shootingRoot.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

        binding.shootingRoot.animate()
            .scaleYBy(0.15f)
            .scaleXBy(-0.15f)
            .translationY(-8 * density)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        val window = dialog.window ?: return

        window.decorView.animate()
            .scaleYBy(-0.15f)
            .scaleXBy(0.15f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        startShootingCallback()
        shoot()
    }

    fun nextStep() = shoot()

    @SuppressLint("SetTextI18n")
    private fun shoot() {
        if (binding.shootingProgress.progress != binding.shootingProgress.max) {
            binding.shootingProgress.setProgress(binding.shootingProgress.progress + 1, true)

            binding.shootingProgressText.text = "(" +
                    (binding.shootingProgress.progress) +
                    "/" +
                    binding.shootingProgress.max + ")"
        }
    }

    fun counterComplete() = with(dialog) {
        setCancelable(true)
        dismiss()
    }

    fun show() = dialog.show()
}