package com.lamia.assistive.speech

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Manages AAC voice announcements for the 4 directional AAC quadrants.
 * Features:
 * - Plays local high-fidelity audio recordings from res/raw if present (Voice Banking support)
 * - Falls back to Android Text-to-Speech (TTS) with natural Malay/English phrasing
 * - Triggers an audible chime alarm tone for KECEMASAN / HELP before speaking
 * - Provides distinctive haptic feedback per action
 */
class AacSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "LAMIA_TTS"
    }

    enum class AacKeyword(
        val labelEn: String,
        val labelMs: String,
        val phraseTemplate: String,
        val rawResName: String
    ) {
        EAT(
            "MAKAN / MINUM",
            "Makan & Minum",
            "%s, saya nak makan, dahaga nak minum air.",
            "aac_eat"
        ),
        TOILET(
            "TANDAS / SELESA",
            "Tandas & Posisi",
            "%s, tolong bawa ke tandas, betulkan posisi saya.",
            "aac_toilet"
        ),
        HELP(
            "KECEMASAN 🚨",
            "Tolong!",
            "%s, tolong saya! Ada kecemasan!",
            "aac_help"
        ),
        LOVE(
            "KASIH SAYANG ❤️",
            "Terima Kasih",
            "Terima kasih selalu jaga saya. Minta maaf susahkan %s... I love you.",
            "aac_love"
        );

        fun getSpokenPhrase(caregiverName: String = "BABA"): String {
            return try {
                String.format(phraseTemplate, caregiverName)
            } catch (e: Exception) {
                String.format(phraseTemplate, "BABA")
            }
        }

        // Backward compatibility property
        val spokenPhrase: String
            get() = getSpokenPhrase("BABA")
    }

    private val userSettings = com.lamia.assistive.config.UserSettings(context)
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var lastSpokenTime = 0L
    private var activeMediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ms", "MY"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English US
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(userSettings.speechRate)
            tts?.setPitch(userSettings.speechPitch)
            isInitialized = true
            Log.d(TAG, "TTS Engine Initialized successfully")
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    fun getSpokenPhrase(keyword: AacKeyword): String {
        return keyword.getSpokenPhrase(userSettings.caregiverName)
    }

    /**
     * Speaks the AAC phrase and triggers haptic feedback.
     * If a corresponding recorded audio file exists in res/raw, plays that;
     * otherwise falls back to Text-To-Speech.
     */
    fun speak(keyword: AacKeyword) {
        val now = System.currentTimeMillis()
        if (now - lastSpokenTime < 1500) {
            return // Debounce cooldown
        }
        lastSpokenTime = now

        if (keyword == AacKeyword.HELP) {
            triggerEmergencyFeedback()
            playEmergencyChime()
            // Delay voice announcement slightly so the loud chime alarm sounds first
            mainHandler.postDelayed({
                executePlayback(keyword)
            }, 450)
        } else {
            triggerHaptic()
            executePlayback(keyword)
        }
    }

    private fun executePlayback(keyword: AacKeyword) {
        // Check if user has provided custom recorded audio in res/raw (Voice Banking)
        val rawResId = context.resources.getIdentifier(keyword.rawResName, "raw", context.packageName)
        if (rawResId != 0) {
            try {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(context, rawResId).apply {
                    setOnCompletionListener {
                        it.release()
                        activeMediaPlayer = null
                    }
                    start()
                }
                Log.d(TAG, "Playing recorded audio for ${keyword.name}")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to play raw audio for ${keyword.name}, falling back to TTS", e)
            }
        }

        // Fallback to Android TTS with personalized caregiver name
        if (isInitialized && tts != null) {
            val phrase = keyword.getSpokenPhrase(userSettings.caregiverName)
            tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, keyword.name)
        }
    }

    /**
     * Speaks a custom announcement phrase (e.g., mode activation).
     */
    fun speakPhrase(phrase: String) {
        triggerHaptic()
        if (isInitialized && tts != null) {
            tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "CUSTOM_ANNOUNCE")
        }
    }

    /**
     * Plays a distinct alert chime tone for emergencies.
     */
    private var emergencyToneGen: ToneGenerator? = null

    private fun playEmergencyChime() {
        try {
            // Release any previous instance before creating a new one
            emergencyToneGen?.release()
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            emergencyToneGen = toneGen
            toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 380)
            // Release after tone playback completes
            mainHandler.postDelayed({
                try {
                    toneGen.release()
                    if (emergencyToneGen === toneGen) emergencyToneGen = null
                } catch (ignored: Exception) {}
            }, 450)
        } catch (e: Exception) {
            Log.w(TAG, "Chime tone generator failed", e)
        }
    }

    private fun triggerHaptic() {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic feedback unavailable", e)
        }
    }

    private fun triggerEmergencyFeedback() {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 250, 100, 250), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Emergency haptic feedback unavailable", e)
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun shutdown() {
        try {
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (ignored: Exception) {}
        try {
            emergencyToneGen?.release()
            emergencyToneGen = null
        } catch (ignored: Exception) {}
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
