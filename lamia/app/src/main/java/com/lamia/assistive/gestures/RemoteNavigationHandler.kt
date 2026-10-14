package com.lamia.assistive.gestures

import com.lamia.assistive.config.GestureConfig
import com.lamia.assistive.vision.HeadPoseDirection
import kotlin.math.abs

/**
 * Handles TV and PC navigation events derived from head pose gestures:
 * 1. Directional D-Pad arrow keys with configurable initial repeat delay and auto-repeat interval.
 * 2. Lateral ear-to-shoulder roll actions (Roll Right -> BACK/ESC, Roll Left -> PLAY/PAUSE).
 */
class RemoteNavigationHandler(
    var config: GestureConfig,
    private val onDispatchDpad: (direction: HeadPoseDirection) -> Unit,
    private val onTriggerBack: () -> Unit,
    private val onTriggerPlayPause: () -> Unit
) {
    private var lastNavDirection = HeadPoseDirection.CENTER
    private var navDirectionStartTime = 0L
    private var lastNavRepeatTime = 0L

    // Lateral Roll State
    private var hasReturnedToCenterFromRoll = true
    private var rollLeftHoldFrames = 0
    private var rollRightHoldFrames = 0
    private var lastRollTriggerTimestamp = 0L

    fun reset() {
        lastNavDirection = HeadPoseDirection.CENTER
        navDirectionStartTime = 0L
        lastNavRepeatTime = 0L
        hasReturnedToCenterFromRoll = true
        rollLeftHoldFrames = 0
        rollRightHoldFrames = 0
    }

    fun handleNavigation(direction: HeadPoseDirection, now: Long = System.currentTimeMillis()) {
        if (direction == HeadPoseDirection.CENTER) {
            lastNavDirection = HeadPoseDirection.CENTER
            navDirectionStartTime = 0L
            lastNavRepeatTime = 0L
            return
        }

        if (direction != lastNavDirection) {
            // New directional tilt: dispatch immediately
            lastNavDirection = direction
            navDirectionStartTime = now
            lastNavRepeatTime = now
            onDispatchDpad(direction)
        } else {
            // Holding directional tilt: auto-repeat
            val holdDuration = now - navDirectionStartTime
            if (holdDuration >= config.navInitialRepeatDelayMs) {
                if (now - lastNavRepeatTime >= config.navRepeatIntervalMs) {
                    lastNavRepeatTime = now
                    onDispatchDpad(direction)
                }
            }
        }
    }

    fun handleHeadRoll(roll: Float, now: Long = System.currentTimeMillis()) {
        if (roll > config.rollThresholdDegrees) {
            // Tilting ear toward RIGHT shoulder -> BACK / ESC
            rollLeftHoldFrames = 0
            if (hasReturnedToCenterFromRoll && now - lastRollTriggerTimestamp > config.rollCooldownMs) {
                rollRightHoldFrames++
                if (rollRightHoldFrames >= config.requiredRollHoldFrames) {
                    hasReturnedToCenterFromRoll = false
                    rollRightHoldFrames = 0
                    lastRollTriggerTimestamp = now
                    onTriggerBack()
                }
            }
        } else if (roll < -config.rollThresholdDegrees) {
            // Tilting ear toward LEFT shoulder -> PLAY / PAUSE
            rollRightHoldFrames = 0
            if (hasReturnedToCenterFromRoll && now - lastRollTriggerTimestamp > config.rollCooldownMs) {
                rollLeftHoldFrames++
                if (rollLeftHoldFrames >= config.requiredRollHoldFrames) {
                    hasReturnedToCenterFromRoll = false
                    rollLeftHoldFrames = 0
                    lastRollTriggerTimestamp = now
                    onTriggerPlayPause()
                }
            }
        } else if (abs(roll) <= config.rollDeadzoneCenterDegrees) {
            hasReturnedToCenterFromRoll = true
            rollLeftHoldFrames = 0
            rollRightHoldFrames = 0
        }
    }
}
