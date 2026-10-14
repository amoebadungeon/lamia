package com.lamia.assistive.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages personal user preferences:
 * - Caregiver's title/name (default: "BABA", customizable to "IBU", "MAMA", "NURSE", "SAYANG", etc.)
 * - Patient's name
 * - Preferred language (ms / en)
 * - Voice speech rate & pitch
 */
class UserSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var caregiverName: String
        get() = prefs.getString(KEY_CAREGIVER_NAME, DEFAULT_CAREGIVER_NAME) ?: DEFAULT_CAREGIVER_NAME
        set(value) = prefs.edit().putString(KEY_CAREGIVER_NAME, value.trim()).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var preferredLanguage: String
        get() = prefs.getString(KEY_PREFERRED_LANG, "ms") ?: "ms"
        set(value) = prefs.edit().putString(KEY_PREFERRED_LANG, value).apply()

    companion object {
        private const val PREFS_NAME = "lamia_user_settings"
        private const val KEY_CAREGIVER_NAME = "caregiver_name"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_PREFERRED_LANG = "preferred_language"

        const val DEFAULT_CAREGIVER_NAME = "BABA"
    }
}
