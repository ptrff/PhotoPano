package ru.ptrff.photopano.main

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ptrff.photopano.R

class OnboardingDialog : DialogFragment() {
    private var currentStep = 0

    private lateinit var alertDialog: AlertDialog

    private val steps = listOf(
        Pair(R.string.onboarding_title_1, R.string.onboarding_message_1),
        Pair(R.string.onboarding_title_2, R.string.onboarding_message_2),
        Pair(R.string.onboarding_title_3, R.string.onboarding_message_3),
        Pair(R.string.onboarding_title_4, R.string.onboarding_message_4),
        Pair(R.string.onboarding_title_5, R.string.onboarding_message_5)
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(steps[currentStep].first))
            .setMessage(getString(steps[currentStep].second))
            .setPositiveButton(getString(R.string.onboarding_next)) { _, _ -> }
            .setNegativeButton(getString(R.string.onboarding_skip)) { _, _ -> }
            .create()

        alertDialog.setOnShowListener { setupButtons() }

        return alertDialog
    }

    private fun setupButtons() {
        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        positiveButton.setOnClickListener {
            if (currentStep < steps.size - 1) {
                currentStep++
                updateDialogContent()
            } else {
                dismiss()
            }
        }

        negativeButton.setOnClickListener { dismiss() }

        if (currentStep > 0) {
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                getString(R.string.onboarding_back)
            ) { _, _ -> }

            val neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            neutralButton.setOnClickListener {
                if (currentStep > 0) {
                    currentStep--
                    updateDialogContent()
                }
            }
        }

        updateButtonTexts()
    }

    private fun updateDialogContent() {
        val (titleRes, messageRes) = steps[currentStep]

        alertDialog.setTitle(getString(titleRes))
        alertDialog.setMessage(getString(messageRes))

        updateButtonTexts()

        val neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)

        if (currentStep == 0) {
            neutralButton?.setOnClickListener(null)
            neutralButton?.visibility = View.GONE
        } else {
            neutralButton?.setOnClickListener {
                if (currentStep > 0) {
                    currentStep--
                    updateDialogContent()
                }
            }
            neutralButton?.visibility = View.VISIBLE
        }
    }

    private fun updateButtonTexts() {
        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)

        positiveButton.text = getString(
            if (currentStep == steps.size - 1) {
                R.string.onboarding_done
            } else {
                R.string.onboarding_next
            }
        )

        neutralButton?.text = getString(R.string.onboarding_back)
    }
}
