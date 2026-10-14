package com.lamia.assistive.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.core.content.ContextCompat
import com.lamia.assistive.R
import com.lamia.assistive.databinding.ActivityMainBinding
import com.lamia.assistive.speech.AacSpeechManager
import com.lamia.assistive.vision.HeadPoseDirection

/**
 * Manages all UI styling, button highlights, status pill updates,
 * tactile haptic animations, and guidance messaging for LAMIA.
 */
class UiStateManager(
    private val context: Context,
    private val binding: ActivityMainBinding
) {

    fun setTrackingStatus(detected: Boolean, pitch: Int = 0, yaw: Int = 0, roll: Int = 0) {
        if (!detected) {
            binding.dotTracking.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lamia_red))
            binding.tvTrackingStatus.setText(R.string.tracking_searching)
        } else {
            binding.dotTracking.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lamia_green))
            binding.tvTrackingStatus.text = context.getString(R.string.tracking_active_format, pitch, yaw, roll)
        }
    }

    fun setMouthState(isLocked: Boolean, isMouthOpen: Boolean, progress: Int) {
        binding.mouthProgressBar.progress = progress

        if (isLocked) {
            binding.tvMouthState.setText(R.string.state_locked)
            binding.tvMouthState.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lamia_red))
            binding.tvMouthState.setTextColor(ContextCompat.getColor(context, R.color.white))
            return
        }

        if (isMouthOpen) {
            binding.tvMouthState.setText(R.string.state_open)
            binding.tvMouthState.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lamia_green))
            binding.tvMouthState.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            binding.tvMouthState.setText(R.string.state_closed)
            binding.tvMouthState.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            binding.tvMouthState.setTextColor(ContextCompat.getColor(context, R.color.dark_bg))
        }
    }

    fun setSmileState(isSmiling: Boolean) {
        if (isSmiling) {
            binding.tvSmileState.setText(R.string.state_smiling)
            binding.tvSmileState.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lamia_green))
            binding.tvSmileState.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            binding.tvSmileState.setText(R.string.state_neutral)
            binding.tvSmileState.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            binding.tvSmileState.setTextColor(ContextCompat.getColor(context, R.color.dark_bg))
        }
    }

    fun updateModeTabs(isRemoteMode: Boolean) {
        if (isRemoteMode) {
            binding.tabRemote.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            binding.tabRemote.setTextColor(ContextCompat.getColor(context, R.color.black))
            binding.tabAac.setBackgroundColor(0)
            binding.tabAac.setTextColor(ContextCompat.getColor(context, R.color.dark_bg))
            binding.containerRemote.visibility = View.VISIBLE
            binding.containerAac.visibility = View.GONE
            binding.tvGuidance.setText(R.string.guidance_remote_default)
        } else {
            binding.tabAac.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            binding.tabAac.setTextColor(ContextCompat.getColor(context, R.color.black))
            binding.tabRemote.setBackgroundColor(0)
            binding.tabRemote.setTextColor(ContextCompat.getColor(context, R.color.dark_bg))
            binding.containerAac.visibility = View.VISIBLE
            binding.containerRemote.visibility = View.GONE
            binding.tvGuidance.setText(R.string.guidance_aac_default)
        }
        resetHighlights(false)
    }

    fun updateDirectionHighlight(dir: HeadPoseDirection, isRemoteMode: Boolean) {
        if (isRemoteMode) {
            binding.btnUp.setBackgroundResource(R.drawable.bg_dpad_button)
            binding.btnDown.setBackgroundResource(R.drawable.bg_dpad_button)
            binding.btnLeft.setBackgroundResource(R.drawable.bg_dpad_button)
            binding.btnRight.setBackgroundResource(R.drawable.bg_dpad_button)
            binding.btnOk.setBackgroundResource(R.drawable.bg_dpad_button)

            when (dir) {
                HeadPoseDirection.UP -> binding.btnUp.setBackgroundResource(R.drawable.bg_dpad_active)
                HeadPoseDirection.DOWN -> binding.btnDown.setBackgroundResource(R.drawable.bg_dpad_active)
                HeadPoseDirection.LEFT -> binding.btnLeft.setBackgroundResource(R.drawable.bg_dpad_active)
                HeadPoseDirection.RIGHT -> binding.btnRight.setBackgroundResource(R.drawable.bg_dpad_active)
                HeadPoseDirection.CENTER -> binding.btnOk.setBackgroundResource(R.drawable.bg_dpad_active)
            }
        }
    }

    fun updateAacSelection(keyword: AacSpeechManager.AacKeyword) {
        binding.cardEat.setBackgroundResource(R.drawable.bg_card_eat)
        binding.cardToilet.setBackgroundResource(R.drawable.bg_card_toilet)
        binding.cardLove.setBackgroundResource(R.drawable.bg_card_love)
        binding.cardHelp.setBackgroundResource(R.drawable.bg_card_help)
        binding.cardCenterSpeak.setBackgroundResource(R.drawable.bg_dpad_button)

        when (keyword) {
            AacSpeechManager.AacKeyword.EAT -> binding.cardEat.setBackgroundResource(R.drawable.bg_card_eat_active)
            AacSpeechManager.AacKeyword.TOILET -> binding.cardToilet.setBackgroundResource(R.drawable.bg_card_toilet_active)
            AacSpeechManager.AacKeyword.LOVE -> binding.cardLove.setBackgroundResource(R.drawable.bg_card_love_active)
            AacSpeechManager.AacKeyword.HELP -> binding.cardHelp.setBackgroundResource(R.drawable.bg_card_help_active)
        }

        binding.tvCenterSpeakLabel.text = keyword.name
    }

    fun setAacCenterIdle() {
        binding.cardEat.setBackgroundResource(R.drawable.bg_card_eat)
        binding.cardToilet.setBackgroundResource(R.drawable.bg_card_toilet)
        binding.cardLove.setBackgroundResource(R.drawable.bg_card_love)
        binding.cardHelp.setBackgroundResource(R.drawable.bg_card_help)
        binding.cardCenterSpeak.setBackgroundResource(R.drawable.bg_dpad_active)
        binding.tvCenterSpeakLabel.setText(R.string.aac_center_aim)
    }

    fun resetHighlights(isLocked: Boolean) {
        binding.btnUp.setBackgroundResource(R.drawable.bg_dpad_button)
        binding.btnDown.setBackgroundResource(R.drawable.bg_dpad_button)
        binding.btnLeft.setBackgroundResource(R.drawable.bg_dpad_button)
        binding.btnRight.setBackgroundResource(R.drawable.bg_dpad_button)
        binding.btnOk.setBackgroundResource(R.drawable.bg_dpad_button)

        binding.cardEat.setBackgroundResource(R.drawable.bg_card_eat)
        binding.cardToilet.setBackgroundResource(R.drawable.bg_card_toilet)
        binding.cardLove.setBackgroundResource(R.drawable.bg_card_love)
        binding.cardHelp.setBackgroundResource(R.drawable.bg_card_help)
        binding.cardCenterSpeak.setBackgroundResource(R.drawable.bg_dpad_button)

        binding.containerDpad.alpha = if (isLocked) 0.35f else 1.0f
    }

    fun updateLockUI(locked: Boolean, isRemoteMode: Boolean) {
        if (locked) {
            binding.tvLockStatus.setText(R.string.lock_status_locked)
            binding.tvLockStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lamia_red))
            binding.tvLockStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
            binding.containerDpad.alpha = 0.35f
            binding.tvGuidance.setText(R.string.guidance_locked)
            resetHighlights(true)
        } else {
            binding.tvLockStatus.setText(R.string.lock_status_active)
            binding.tvLockStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lamia_green))
            binding.tvLockStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
            binding.containerDpad.alpha = 1.0f
            if (isRemoteMode) {
                binding.tvGuidance.setText(R.string.guidance_remote_active)
            } else {
                binding.tvGuidance.setText(R.string.guidance_aac_default)
            }
        }
    }

    fun setGuidanceText(text: String) {
        binding.tvGuidance.text = text
    }

    fun animatePress(view: View, action: () -> Unit) {
        vibratePhone(50)
        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(60).withEndAction {
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(60).start()
        }.start()
        action()
    }

    fun vibratePhone(durationMs: Long = 50) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (ignored: Exception) {}
    }

    fun vibrateDouble() {
        vibratePhone(80)
        binding.root.postDelayed({ vibratePhone(140) }, 180)
    }
}
