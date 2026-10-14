package com.lamia.assistive.vision

/**
 * Directional targets based on head tilt and yaw.
 */
enum class HeadPoseDirection {
    CENTER,
    UP,
    DOWN,
    LEFT,
    RIGHT
}

/**
 * Represents the real-time facial gesture data computed by ML Kit.
 */
data class FaceGestureState(
    val direction: HeadPoseDirection = HeadPoseDirection.CENTER,
    val pitch: Float = 0f,              // Euler X (Tilt Up/Down)
    val yaw: Float = 0f,                // Euler Y (Turn Left/Right)
    val roll: Float = 0f,               // Euler Z (Tilt Sideways)
    val mouthOpenRatio: Float = 0f,     // Normalized lip separation
    val isMouthOpen: Boolean = false,   // True when mouth is opened past threshold
    val smilingProbability: Float = 0f, // Smile probability (0.0 to 1.0)
    val isSmiling: Boolean = false,     // True when smile exceeds threshold
    val faceDetected: Boolean = false,
    val rightEyeOpenProb: Float = 0f,
    val leftEyeOpenProb: Float = 0f
)
