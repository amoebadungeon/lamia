package com.lamia.assistive.gestures

import com.lamia.assistive.config.GestureConfig
import com.lamia.assistive.vision.HeadPoseDirection
import kotlin.math.abs

/**
 * Detects intentional 5-gaze sequences to switch between Smart Remote mode
 * and Quick Speak (AAC) mode.
 *
 * Requirements:
 * - 5 intentional glances in the target direction (Left -> AAC, Right -> Remote).
 * - Mandatory return-to-center deadzone (|yaw| < deadzone) between glances.
 * - Sustained glance hold (~100-150ms) to filter out single-frame twitches or tremors.
 * - Sliding window expiration timeout (e.g. 2.8s) resets counter if abandoned.
 */
class ModeSwitchDetector(
    var config: GestureConfig,
    private val onLeftGazeProgress: (count: Int) -> Unit,
    private val onRightGazeProgress: (count: Int) -> Unit,
    private val onActivateAac: () -> Unit,
    private val onActivateRemote: () -> Unit,
    private val onSequenceExpired: (isRemoteMode: Boolean) -> Unit
) {
    private var leftGazeCount = 0
    private var lastLeftGazeTimestamp = 0L
    private var hasReturnedToCenterFromLeft = true
    private var leftGazeHoldFrames = 0

    private var rightGazeCount = 0
    private var lastRightGazeTimestamp = 0L
    private var hasReturnedToCenterFromRight = true
    private var rightGazeHoldFrames = 0

    fun reset() {
        leftGazeCount = 0
        rightGazeCount = 0
        leftGazeHoldFrames = 0
        rightGazeHoldFrames = 0
        hasReturnedToCenterFromLeft = true
        hasReturnedToCenterFromRight = true
    }

    fun processFrame(
        direction: HeadPoseDirection,
        yaw: Float,
        isRemoteMode: Boolean,
        currentTime: Long
    ) {
        // Expiration check
        if (leftGazeCount > 0 && currentTime - lastLeftGazeTimestamp > config.gazeSequenceTimeoutMs) {
            leftGazeCount = 0
            if (isRemoteMode) onSequenceExpired(true)
        }
        if (rightGazeCount > 0 && currentTime - lastRightGazeTimestamp > config.gazeSequenceTimeoutMs) {
            rightGazeCount = 0
            if (!isRemoteMode) onSequenceExpired(false)
        }

        // Return-to-Center Hysteresis Deadzone
        if (abs(yaw) < config.gazeCenterDeadzoneDegrees || direction == HeadPoseDirection.CENTER) {
            hasReturnedToCenterFromLeft = true
            hasReturnedToCenterFromRight = true
        }

        // 5 Voluntary Gazes to the Left -> Activate AAC
        if (direction == HeadPoseDirection.LEFT && isRemoteMode) {
            leftGazeHoldFrames++
            if (leftGazeHoldFrames >= config.requiredGazeHoldFrames && hasReturnedToCenterFromLeft) {
                hasReturnedToCenterFromLeft = false
                leftGazeHoldFrames = 0
                leftGazeCount++
                lastLeftGazeTimestamp = currentTime

                onLeftGazeProgress(leftGazeCount)

                if (leftGazeCount >= 5) {
                    leftGazeCount = 0
                    onActivateAac()
                }
            }
        } else {
            leftGazeHoldFrames = 0
        }

        // 5 Voluntary Gazes to the Right while in AAC -> Return to Remote Mode
        if (direction == HeadPoseDirection.RIGHT && !isRemoteMode) {
            rightGazeHoldFrames++
            if (rightGazeHoldFrames >= config.requiredGazeHoldFrames && hasReturnedToCenterFromRight) {
                hasReturnedToCenterFromRight = false
                rightGazeHoldFrames = 0
                rightGazeCount++
                lastRightGazeTimestamp = currentTime

                onRightGazeProgress(rightGazeCount)

                if (rightGazeCount >= 5) {
                    rightGazeCount = 0
                    onActivateRemote()
                }
            }
        } else {
            rightGazeHoldFrames = 0
        }
    }
}
