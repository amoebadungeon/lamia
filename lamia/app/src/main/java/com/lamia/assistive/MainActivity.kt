package com.lamia.assistive

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.lamia.assistive.bluetooth.BluetoothController
import com.lamia.assistive.bluetooth.reports.KeyboardReport
import com.lamia.assistive.config.GestureConfig
import com.lamia.assistive.config.UserSettings
import com.lamia.assistive.databinding.ActivityMainBinding
import com.lamia.assistive.gestures.LockGestureDetector
import com.lamia.assistive.gestures.ModeSwitchDetector
import com.lamia.assistive.gestures.RemoteNavigationHandler
import com.lamia.assistive.speech.AacSpeechManager
import com.lamia.assistive.ui.UiStateManager
import com.lamia.assistive.vision.FaceGestureAnalyzer
import com.lamia.assistive.vision.FaceGestureState
import com.lamia.assistive.vision.HeadPoseDirection
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Main Activity for LAMIA Assistive Controller.
 * Coordinates CameraX, Bluetooth HID, gesture recognition pipeline, and UI state.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var gestureConfig: GestureConfig
    private lateinit var userSettings: UserSettings
    private lateinit var speechManager: AacSpeechManager
    private lateinit var gestureAnalyzer: FaceGestureAnalyzer
    private lateinit var uiStateManager: UiStateManager

    // Modular Gesture Processors
    private lateinit var modeSwitchDetector: ModeSwitchDetector
    private lateinit var remoteNavHandler: RemoteNavigationHandler
    private lateinit var lockGestureDetector: LockGestureDetector

    private var isRemoteMode = true
    private var currentDirection = HeadPoseDirection.CENTER
    private var currentAacKeyword = AacSpeechManager.AacKeyword.EAT
    private var lastMouthTriggerTime = 0L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val btGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        } else true

        if (cameraGranted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.toast_camera_perm, Toast.LENGTH_SHORT).show()
        }

        if (btGranted) {
            BluetoothController.init(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gestureConfig = GestureConfig.load(this)
        userSettings = UserSettings(this)
        speechManager = AacSpeechManager(this)
        uiStateManager = UiStateManager(this, binding)

        setupGestureModules()
        setupBluetooth()
        setupUI()
        checkAndRequestPermissions()
    }

    private fun setupGestureModules() {
        modeSwitchDetector = ModeSwitchDetector(
            config = gestureConfig,
            onLeftGazeProgress = { count ->
                uiStateManager.vibratePhone(45)
                uiStateManager.setGuidanceText(getString(R.string.gaze_progress_aac, count))
            },
            onRightGazeProgress = { count ->
                uiStateManager.vibratePhone(45)
                uiStateManager.setGuidanceText(getString(R.string.gaze_progress_remote, count))
            },
            onActivateAac = {
                uiStateManager.vibrateDouble()
                switchMode(remote = false)
                speechManager.speakPhrase(getString(R.string.tab_aac))
                Toast.makeText(this, R.string.toast_aac_activated, Toast.LENGTH_LONG).show()
            },
            onActivateRemote = {
                uiStateManager.vibrateDouble()
                switchMode(remote = true)
                speechManager.speakPhrase(getString(R.string.tab_remote))
                Toast.makeText(this, R.string.toast_remote_activated, Toast.LENGTH_SHORT).show()
            },
            onSequenceExpired = { remote ->
                uiStateManager.setGuidanceText(
                    if (remote) getString(R.string.guidance_remote_default)
                    else getString(R.string.guidance_aac_default)
                )
            }
        )

        remoteNavHandler = RemoteNavigationHandler(
            config = gestureConfig,
            onDispatchDpad = { dir -> dispatchDpadKey(dir) },
            onTriggerBack = { triggerBackButton() },
            onTriggerPlayPause = { triggerPlayPauseButton() }
        )

        lockGestureDetector = LockGestureDetector(
            config = gestureConfig,
            onToggleLock = { locked ->
                uiStateManager.updateLockUI(locked, isRemoteMode)
                if (locked) {
                    uiStateManager.vibratePhone(150)
                    speechManager.speakPhrase(getString(R.string.state_locked))
                    Toast.makeText(this, R.string.toast_locked, Toast.LENGTH_SHORT).show()
                } else {
                    uiStateManager.vibrateDouble()
                    speechManager.speakPhrase(getString(R.string.lock_status_active))
                    Toast.makeText(this, R.string.toast_unlocked, Toast.LENGTH_SHORT).show()
                }
            }
        )

        gestureAnalyzer = FaceGestureAnalyzer(gestureConfig) { gestureState ->
            mainHandler.post { handleGesture(gestureState) }
        }
    }

    private fun setupBluetooth() {
        BluetoothController.onAppRegisteredListener = { registered ->
            binding.tvBluetoothStatus.text = if (registered) {
                getString(R.string.bt_status_ready)
            } else {
                getString(R.string.bt_status_not_registered)
            }
            binding.tvBluetoothStatus.setTextColor(
                if (registered) ContextCompat.getColor(this, R.color.lamia_green)
                else ContextCompat.getColor(this, R.color.lamia_blue)
            )
        }

        BluetoothController.onConnectionStateListener = { state, device ->
            val isConnected = state == BluetoothProfile.STATE_CONNECTED
            val devName = try { device?.name ?: device?.address ?: "Host" } catch (e: Exception) { "Host" }
            if (isConnected) {
                binding.tvBluetoothStatus.text = getString(R.string.bt_status_connected, devName)
                binding.tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, R.color.lamia_green))
                Toast.makeText(this, getString(R.string.toast_bt_connected, devName), Toast.LENGTH_SHORT).show()
            } else {
                binding.tvBluetoothStatus.text = getString(R.string.bt_status_tap_connect)
                binding.tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, R.color.lamia_blue))
            }
        }
    }

    private fun setupUI() {
        // Tab Switching
        binding.tabRemote.setOnClickListener { switchMode(remote = true) }
        binding.tabAac.setOnClickListener { switchMode(remote = false) }

        // Bluetooth Status Pill Click -> Device Picker Dialog
        binding.tvBluetoothStatus.setOnClickListener { showDevicePickerDialog() }

        // Lock / Unlock Status Pill Click -> Manual Toggle
        binding.tvLockStatus.setOnClickListener { lockGestureDetector.toggleManually() }

        // Rest Calibration Button
        binding.btnCalibrate.setOnClickListener {
            gestureAnalyzer.calibrateRestingPosition()
            Toast.makeText(this, R.string.toast_rest_calibrated, Toast.LENGTH_SHORT).show()
        }

        // ================= TOUCH PRESS D-PAD BUTTONS =================
        binding.btnUp.setOnClickListener {
            uiStateManager.animatePress(it) { sendKey(KeyboardReport.KEY_UP, "DPAD_UP") }
        }
        binding.btnDown.setOnClickListener {
            uiStateManager.animatePress(it) { sendKey(KeyboardReport.KEY_DOWN, "DPAD_DOWN") }
        }
        binding.btnLeft.setOnClickListener {
            uiStateManager.animatePress(it) { sendKey(KeyboardReport.KEY_LEFT, "DPAD_LEFT") }
        }
        binding.btnRight.setOnClickListener {
            uiStateManager.animatePress(it) { sendKey(KeyboardReport.KEY_RIGHT, "DPAD_RIGHT") }
        }
        binding.btnOk.setOnClickListener {
            uiStateManager.animatePress(it) { sendKey(KeyboardReport.KEY_ENTER, "ENTER / OK") }
        }
        binding.btnBack.setOnClickListener { triggerBackButton() }
        binding.btnHome.setOnClickListener {
            uiStateManager.animatePress(it) { sendKey(KeyboardReport.KEY_HOME, "HOME") }
        }

        // ================= TOUCH PRESS AAC D-PAD BUTTONS =================
        binding.cardEat.setOnClickListener {
            uiStateManager.animatePress(it) { selectAacKeyword(AacSpeechManager.AacKeyword.EAT, trigger = true) }
        }
        binding.cardToilet.setOnClickListener {
            uiStateManager.animatePress(it) { selectAacKeyword(AacSpeechManager.AacKeyword.TOILET, trigger = true) }
        }
        binding.cardLove.setOnClickListener {
            uiStateManager.animatePress(it) { selectAacKeyword(AacSpeechManager.AacKeyword.LOVE, trigger = true) }
        }
        binding.cardHelp.setOnClickListener {
            uiStateManager.animatePress(it) { selectAacKeyword(AacSpeechManager.AacKeyword.HELP, trigger = true) }
        }
        binding.cardCenterSpeak.setOnClickListener {
            uiStateManager.animatePress(it) {
                speechManager.speak(currentAacKeyword)
                uiStateManager.setGuidanceText(speechManager.getSpokenPhrase(currentAacKeyword))
            }
        }
        binding.btnRepeatVoice.setOnClickListener {
            uiStateManager.animatePress(it) {
                speechManager.speak(currentAacKeyword)
                uiStateManager.setGuidanceText(speechManager.getSpokenPhrase(currentAacKeyword))
            }
        }
        binding.btnAacToRemote.setOnClickListener {
            uiStateManager.animatePress(it) { switchMode(remote = true) }
        }

        // Test Mouth Open Button
        binding.btnSimulateTrigger.setOnClickListener { triggerMouthAction() }
    }

    private fun handleGesture(state: FaceGestureState) {
        if (!state.faceDetected) {
            uiStateManager.setTrackingStatus(false)
            return
        }

        uiStateManager.setTrackingStatus(true, state.pitch.toInt(), state.yaw.toInt(), state.roll.toInt())

        val mouthProgress = (state.mouthOpenRatio * 1000).toInt().coerceIn(0, 100)
        uiStateManager.setMouthState(lockGestureDetector.isControlsLocked, state.isMouthOpen, mouthProgress)
        uiStateManager.setSmileState(state.isSmiling)

        val currentTime = System.currentTimeMillis()

        // 1. Smile Lock Toggle
        lockGestureDetector.processSmile(state.isSmiling, state.smilingProbability, currentTime)

        // 2. Lock Guard: Freeze all controls during video playback if locked
        if (lockGestureDetector.isControlsLocked) return

        // 3. Mouth Opening Trigger (with cooldown)
        if (state.isMouthOpen && currentTime - lastMouthTriggerTime > gestureConfig.mouthCooldownMs) {
            lastMouthTriggerTime = currentTime
            triggerMouthAction()
        }

        // 4. Mode Switch Gesture Detection (5 Gazes)
        modeSwitchDetector.processFrame(state.direction, state.yaw, isRemoteMode, currentTime)

        // 5. Visual D-Pad Highlights
        if (state.direction != currentDirection) {
            currentDirection = state.direction
            updateDirectionHighlight(state.direction)
        }

        // 6. Direct Remote Navigation & Head Roll Gestures
        if (isRemoteMode) {
            val navDir = if (abs(state.roll) < gestureConfig.rollThresholdDegrees) state.direction else HeadPoseDirection.CENTER
            remoteNavHandler.handleNavigation(navDir, currentTime)
            remoteNavHandler.handleHeadRoll(state.roll, currentTime)
        }
    }

    private fun dispatchDpadKey(direction: HeadPoseDirection) {
        when (direction) {
            HeadPoseDirection.UP -> uiStateManager.animatePress(binding.btnUp) {
                sendKey(KeyboardReport.KEY_UP, "DPAD_UP")
            }
            HeadPoseDirection.DOWN -> uiStateManager.animatePress(binding.btnDown) {
                sendKey(KeyboardReport.KEY_DOWN, "DPAD_DOWN")
            }
            HeadPoseDirection.LEFT -> uiStateManager.animatePress(binding.btnLeft) {
                sendKey(KeyboardReport.KEY_LEFT, "DPAD_LEFT")
            }
            HeadPoseDirection.RIGHT -> uiStateManager.animatePress(binding.btnRight) {
                sendKey(KeyboardReport.KEY_RIGHT, "DPAD_RIGHT")
            }
            HeadPoseDirection.CENTER -> {}
        }
    }

    private fun updateDirectionHighlight(dir: HeadPoseDirection) {
        if (isRemoteMode) {
            uiStateManager.updateDirectionHighlight(dir, true)
        } else {
            when (dir) {
                HeadPoseDirection.UP -> selectAacKeyword(AacSpeechManager.AacKeyword.EAT, trigger = false)
                HeadPoseDirection.DOWN -> selectAacKeyword(AacSpeechManager.AacKeyword.HELP, trigger = false)
                HeadPoseDirection.LEFT -> selectAacKeyword(AacSpeechManager.AacKeyword.TOILET, trigger = false)
                HeadPoseDirection.RIGHT -> selectAacKeyword(AacSpeechManager.AacKeyword.LOVE, trigger = false)
                HeadPoseDirection.CENTER -> uiStateManager.setAacCenterIdle()
            }
        }
    }

    private fun selectAacKeyword(keyword: AacSpeechManager.AacKeyword, trigger: Boolean) {
        currentAacKeyword = keyword
        uiStateManager.updateAacSelection(keyword)
        if (trigger) {
            speechManager.speak(keyword)
            uiStateManager.setGuidanceText(speechManager.getSpokenPhrase(keyword))
        }
    }

    private fun triggerMouthAction() {
        if (isRemoteMode) {
            uiStateManager.animatePress(binding.btnOk) {
                sendKey(KeyboardReport.KEY_ENTER, "ENTER / OK")
            }
        } else {
            when (currentDirection) {
                HeadPoseDirection.UP -> uiStateManager.animatePress(binding.cardEat) { speechManager.speak(AacSpeechManager.AacKeyword.EAT) }
                HeadPoseDirection.DOWN -> uiStateManager.animatePress(binding.cardHelp) { speechManager.speak(AacSpeechManager.AacKeyword.HELP) }
                HeadPoseDirection.LEFT -> uiStateManager.animatePress(binding.cardToilet) { speechManager.speak(AacSpeechManager.AacKeyword.TOILET) }
                HeadPoseDirection.RIGHT -> uiStateManager.animatePress(binding.cardLove) { speechManager.speak(AacSpeechManager.AacKeyword.LOVE) }
                HeadPoseDirection.CENTER -> {
                    uiStateManager.animatePress(binding.cardCenterSpeak) {
                        uiStateManager.vibratePhone(40)
                        uiStateManager.setGuidanceText(getString(R.string.guidance_aac_aim_prompt))
                    }
                }
            }
        }
    }

    private fun triggerBackButton() {
        uiStateManager.animatePress(binding.btnBack) {
            val sender = BluetoothController.getKeyboardSender()
            if (sender != null) {
                sender.sendSmartBack { showFeedback("BACK / ESC (Sent)") }
            } else {
                sendKey(KeyboardReport.KEY_ESCAPE, "BACK / ESC")
            }
        }
    }

    private fun triggerPlayPauseButton() {
        uiStateManager.animatePress(binding.btnOk) {
            sendKey(KeyboardReport.KEY_SPACE, "PLAY / PAUSE")
        }
    }

    private fun switchMode(remote: Boolean) {
        isRemoteMode = remote
        modeSwitchDetector.reset()
        remoteNavHandler.reset()
        uiStateManager.updateModeTabs(remote)
    }

    private fun sendKey(keyCode: Byte, keyLabel: String) {
        val sender = BluetoothController.getKeyboardSender()
        if (sender != null) {
            sender.sendKeyPressAndRelease(keyCode) { _ ->
                showFeedback("$keyLabel (Sent)")
            }
        } else {
            showFeedback("$keyLabel (Simulated)")
        }
    }

    private fun showFeedback(actionText: String) {
        val hostName = try { BluetoothController.hostDevice?.name ?: "No Host" } catch (e: Exception) { "Host" }
        uiStateManager.setGuidanceText(getString(R.string.action_feedback_format, actionText, hostName))
    }

    @SuppressLint("MissingPermission")
    private fun showDevicePickerDialog() {
        val bonded = BluetoothController.getBondedDevices()
        val lastMac = BluetoothController.getLastConnectedDeviceMac()
        val options = mutableListOf<String>()

        bonded.forEach {
            val isLast = it.address.equals(lastMac, ignoreCase = true)
            val name = try { it.name ?: it.address } catch (e: Exception) { it.address }
            options.add(if (isLast) getString(R.string.dialog_bt_last_connected, name) else name)
        }
        options.add(getString(R.string.dialog_bt_discoverable))

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_bt_title)
            .setItems(options.toTypedArray()) { _, which ->
                if (which < bonded.size) {
                    val target = bonded[which]
                    val displayName = options[which].replace("★ ", "").replace(" (Last Connected)", "")
                    Toast.makeText(this, getString(R.string.toast_connecting, displayName), Toast.LENGTH_SHORT).show()
                    BluetoothController.connect(target)
                } else {
                    BluetoothController.makeDiscoverable(this)
                    Toast.makeText(this, R.string.toast_discoverable, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            startCamera()
            BluetoothController.init(this)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, gestureAnalyzer)
                }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.camera_start_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (BluetoothController.hostDevice == null) {
            BluetoothController.autoConnectLastDevice()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        BluetoothController.onAppRegisteredListener = null
        BluetoothController.onConnectionStateListener = null
        BluetoothController.release(this)
        speechManager.shutdown()
    }
}
