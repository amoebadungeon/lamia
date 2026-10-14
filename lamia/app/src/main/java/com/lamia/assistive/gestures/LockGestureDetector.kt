package com.lamia.assistive.gestures

import com.lamia.assistive.config.GestureConfig

/**
 * Detects sustained smiles to toggle the controller lock state.
 *
 * When locked, all navigation and mouth click triggers are frozen so the patient
 * can watch movies or rest without accidental keystrokes.
 */
class LockGestureDetector(
    var config: GestureConfig,
    private val onToggleLock: (isLocked: Boolean) -> Unit
) {
    var isControlsLocked: Boolean = false
        private set

    private var smileHoldFrames = 0
    private var hasReleasedSmile = true
    private var lastSmileToggleTimestamp = 0L

    fun toggleManually(): Boolean {
        isControlsLocked = !isControlsLocked
        onToggleLock(isControlsLocked)
        return isControlsLocked
    }

    fun processSmile(
        isSmiling: Boolean,
        smilingProbability: Float,
        currentTime: Long = System.currentTimeMillis()
    ) {
        // Hysteresis: user must relax face before re-arming smile toggle
        if (!isSmiling && smilingProbability < config.smileRelaxThreshold) {
            hasReleasedSmile = true
        }

        if (isSmiling) {
            smileHoldFrames++
            if (smileHoldFrames >= config.requiredSmileHoldFrames &&
                hasReleasedSmile &&
                currentTime - lastSmileToggleTimestamp > config.smileToggleCooldownMs
            ) {
                hasReleasedSmile = false
                smileHoldFrames = 0
                lastSmileToggleTimestamp = currentTime
                isControlsLocked = !isControlsLocked
                onToggleLock(isControlsLocked)
            }
        } else {
            smileHoldFrames = 0
        }
    }
}
