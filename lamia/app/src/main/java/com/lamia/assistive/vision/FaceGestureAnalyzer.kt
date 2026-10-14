package com.lamia.assistive.vision

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

import com.lamia.assistive.config.GestureConfig

/**
 * CameraX ImageAnalysis analyzer that runs Google ML Kit Face Detection.
 * Computes smoothed head pose direction and voluntary mouth-opening ratio.
 */
class FaceGestureAnalyzer(
    var config: GestureConfig = GestureConfig(),
    private val onGestureResult: (FaceGestureState) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.2f)
            .build()
    )

    // Calibration offsets for user's natural resting head position
    @Volatile var pitchOffset: Float = 0f
    @Volatile var yawOffset: Float = 0f
    @Volatile var rollOffset: Float = 0f
    private var isInitialCalibrated = false
    private var warmupFrames = 0

    // Smoothed values for tremor filtering
    private var smoothedPitch = 0f
    private var smoothedYaw = 0f
    private var smoothedRoll = 0f
    private var smoothedMouthRatio = 0f
    private var smoothedSmilingProb = 0f

    /**
     * Calibrates current head position as the natural center (0, 0).
     */
    fun calibrateRestingPosition() {
        pitchOffset = smoothedPitch
        yawOffset = smoothedYaw
        rollOffset = smoothedRoll
        isInitialCalibrated = true
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    processFace(faces.first())
                } else {
                    onGestureResult(FaceGestureState(faceDetected = false))
                }
            }
            .addOnFailureListener {
                onGestureResult(FaceGestureState(faceDetected = false))
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processFace(face: Face) {
        val rawPitch = face.headEulerAngleX
        val rawYaw = -face.headEulerAngleY // Invert for mirrored front-camera preview
        val rawRoll = -face.headEulerAngleZ // Invert for mirrored front-camera preview (tilt right = positive, tilt left = negative)

        // Apply Exponential Moving Average (EMA) to filter involuntary muscle tremors
        smoothedPitch += (rawPitch - smoothedPitch) * config.smoothingFactor
        smoothedYaw += (rawYaw - smoothedYaw) * config.smoothingFactor
        smoothedRoll += (rawRoll - smoothedRoll) * config.smoothingFactor

        // Auto-calibrate natural resting position during initial steady detection (~0.3s)
        if (!isInitialCalibrated) {
            warmupFrames++
            if (warmupFrames >= 10) {
                pitchOffset = smoothedPitch
                yawOffset = smoothedYaw
                rollOffset = smoothedRoll
                isInitialCalibrated = true
            }
        }

        // Calculate delta relative to calibrated rest position
        val adjustedPitch = smoothedPitch - pitchOffset
        val adjustedYaw = smoothedYaw - yawOffset
        val adjustedRoll = smoothedRoll - rollOffset

        // Compute Mouth-Open Ratio using inner lip contours
        val mouthRatio = calculateMouthOpenRatio(face)
        smoothedMouthRatio += (mouthRatio - smoothedMouthRatio) * 0.45f

        val isMouthOpen = smoothedMouthRatio >= config.mouthOpenThreshold

        // Compute Smile Probability from ML Kit
        val rawSmiling = face.smilingProbability ?: 0f
        smoothedSmilingProb += (rawSmiling - smoothedSmilingProb) * 0.45f
        val isSmiling = smoothedSmilingProb >= config.smileThreshold

        // Classify directional aim (with roll guard to prevent phantom yaw when tilting ear to shoulder)
        val direction = classifyDirection(adjustedPitch, adjustedYaw, adjustedRoll)

        val state = FaceGestureState(
            direction = direction,
            pitch = adjustedPitch,
            yaw = adjustedYaw,
            roll = adjustedRoll,
            mouthOpenRatio = smoothedMouthRatio,
            isMouthOpen = isMouthOpen,
            smilingProbability = smoothedSmilingProb,
            isSmiling = isSmiling,
            faceDetected = true,
            rightEyeOpenProb = face.rightEyeOpenProbability ?: 0f,
            leftEyeOpenProb = face.leftEyeOpenProbability ?: 0f
        )

        onGestureResult(state)
    }

    private fun calculateMouthOpenRatio(face: Face): Float {
        val upperLipBottom = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points
        val lowerLipTop = face.getContour(FaceContour.LOWER_LIP_TOP)?.points
        val faceHeight = face.boundingBox.height().toFloat()

        if (upperLipBottom.isNullOrEmpty() || lowerLipTop.isNullOrEmpty() || faceHeight <= 0f) {
            return 0f
        }

        // Measure vertical gap between center upper and lower lip points
        val upperCenterY = upperLipBottom[upperLipBottom.size / 2].y
        val lowerCenterY = lowerLipTop[lowerLipTop.size / 2].y
        val gap = abs(lowerCenterY - upperCenterY)

        return gap / faceHeight
    }

    private fun classifyDirection(pitch: Float, yaw: Float, roll: Float): HeadPoseDirection {
        // If user is tilting their head sideways (ear to shoulder), this is a lateral ROLL action (BACK / PLAY-PAUSE),
        // NOT a D-PAD arrow navigation! Suppress arrow keys to prevent false triggers from phantom yaw.
        if (abs(roll) >= 8.0f) {
            return HeadPoseDirection.CENTER
        }

        // Compute normalized activation ratios (> 1.0 means past threshold)
        val pitchRatio = when {
            pitch > config.pitchThresholdUp -> pitch / config.pitchThresholdUp
            pitch < config.pitchThresholdDown -> pitch / config.pitchThresholdDown
            else -> 0f
        }

        val yawRatio = when {
            yaw > config.yawThresholdRight -> yaw / config.yawThresholdRight
            yaw < config.yawThresholdLeft -> yaw / config.yawThresholdLeft
            else -> 0f
        }

        // If neither axis breached its deadzone threshold, return CENTER
        if (pitchRatio <= 0f && yawRatio <= 0f) {
            return HeadPoseDirection.CENTER
        }

        // Prioritize whichever axis had the stronger intentional movement relative to its threshold
        return if (pitchRatio >= yawRatio) {
            if (pitch > 0) HeadPoseDirection.UP else HeadPoseDirection.DOWN
        } else {
            if (yaw > 0) HeadPoseDirection.RIGHT else HeadPoseDirection.LEFT
        }
    }
}
