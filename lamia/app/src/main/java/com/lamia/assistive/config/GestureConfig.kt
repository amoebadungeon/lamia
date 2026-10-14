package com.lamia.assistive.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralized, customizable configuration for all gesture detection thresholds,
 * cooldown timers, and auto-repeat intervals.
 *
 * Persisted in SharedPreferences so users and caregivers can tune sensitivity
 * to match varying motor control capabilities.
 */
data class GestureConfig(
    // 5-Gaze Mode Switch Timing
    var gazeSequenceTimeoutMs: Long = 2800L,
    var requiredGazeHoldFrames: Int = 3,         // ~100-150ms sustained glance
    var gazeCenterDeadzoneDegrees: Float = 8.0f, // Must return within center before next count

    // D-Pad Auto-Repeat Timing
    var navInitialRepeatDelayMs: Long = 450L,
    var navRepeatIntervalMs: Long = 350L,

    // Smile Controller Lock Toggle
    var requiredSmileHoldFrames: Int = 3,
    var smileToggleCooldownMs: Long = 1200L,
    var smileRelaxThreshold: Float = 0.30f,

    // Head Roll Gestures (Lateral Ear-to-Shoulder Tilt)
    var rollThresholdDegrees: Float = 9.5f,
    var rollDeadzoneCenterDegrees: Float = 4.5f,
    var requiredRollHoldFrames: Int = 3,
    var rollCooldownMs: Long = 800L,

    // Mouth Trigger Cooldown
    var mouthCooldownMs: Long = 1000L,

    // Vision Sensitivity
    var pitchThresholdUp: Float = 12.0f,
    var pitchThresholdDown: Float = -8.0f,
    var yawThresholdRight: Float = 14.0f,
    var yawThresholdLeft: Float = -14.0f,
    var mouthOpenThreshold: Float = 0.055f,
    var smileThreshold: Float = 0.55f,
    var smoothingFactor: Float = 0.35f
) {
    companion object {
        private const val PREFS_NAME = "lamia_gesture_config"

        fun load(context: Context): GestureConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return GestureConfig(
                gazeSequenceTimeoutMs = prefs.getLong("gazeSequenceTimeoutMs", 2800L),
                requiredGazeHoldFrames = prefs.getInt("requiredGazeHoldFrames", 3),
                gazeCenterDeadzoneDegrees = prefs.getFloat("gazeCenterDeadzoneDegrees", 8.0f),
                navInitialRepeatDelayMs = prefs.getLong("navInitialRepeatDelayMs", 450L),
                navRepeatIntervalMs = prefs.getLong("navRepeatIntervalMs", 350L),
                requiredSmileHoldFrames = prefs.getInt("requiredSmileHoldFrames", 3),
                smileToggleCooldownMs = prefs.getLong("smileToggleCooldownMs", 1200L),
                smileRelaxThreshold = prefs.getFloat("smileRelaxThreshold", 0.30f),
                rollThresholdDegrees = prefs.getFloat("rollThresholdDegrees", 9.5f),
                rollDeadzoneCenterDegrees = prefs.getFloat("rollDeadzoneCenterDegrees", 4.5f),
                requiredRollHoldFrames = prefs.getInt("requiredRollHoldFrames", 3),
                rollCooldownMs = prefs.getLong("rollCooldownMs", 800L),
                mouthCooldownMs = prefs.getLong("mouthCooldownMs", 1000L),
                pitchThresholdUp = prefs.getFloat("pitchThresholdUp", 12.0f),
                pitchThresholdDown = prefs.getFloat("pitchThresholdDown", -8.0f),
                yawThresholdRight = prefs.getFloat("yawThresholdRight", 14.0f),
                yawThresholdLeft = prefs.getFloat("yawThresholdLeft", -14.0f),
                mouthOpenThreshold = prefs.getFloat("mouthOpenThreshold", 0.055f),
                smileThreshold = prefs.getFloat("smileThreshold", 0.55f),
                smoothingFactor = prefs.getFloat("smoothingFactor", 0.35f)
            )
        }
    }

    fun save(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("gazeSequenceTimeoutMs", gazeSequenceTimeoutMs)
            putInt("requiredGazeHoldFrames", requiredGazeHoldFrames)
            putFloat("gazeCenterDeadzoneDegrees", gazeCenterDeadzoneDegrees)
            putLong("navInitialRepeatDelayMs", navInitialRepeatDelayMs)
            putLong("navRepeatIntervalMs", navRepeatIntervalMs)
            putInt("requiredSmileHoldFrames", requiredSmileHoldFrames)
            putLong("smileToggleCooldownMs", smileToggleCooldownMs)
            putFloat("smileRelaxThreshold", smileRelaxThreshold)
            putFloat("rollThresholdDegrees", rollThresholdDegrees)
            putFloat("rollDeadzoneCenterDegrees", rollDeadzoneCenterDegrees)
            putInt("requiredRollHoldFrames", requiredRollHoldFrames)
            putLong("rollCooldownMs", rollCooldownMs)
            putLong("mouthCooldownMs", mouthCooldownMs)
            putFloat("pitchThresholdUp", pitchThresholdUp)
            putFloat("pitchThresholdDown", pitchThresholdDown)
            putFloat("yawThresholdRight", yawThresholdRight)
            putFloat("yawThresholdLeft", yawThresholdLeft)
            putFloat("mouthOpenThreshold", mouthOpenThreshold)
            putFloat("smileThreshold", smileThreshold)
            putFloat("smoothingFactor", smoothingFactor)
            apply()
        }
    }
}
