package com.aawararide.settings

import android.content.Context
import android.content.SharedPreferences

class AnnouncementSettingsRepository(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isInitialEnabled() = preferences.getBoolean(INITIAL, true)
    fun isRepeatEnabled() = preferences.getBoolean(REPEAT, true)
    fun getRepeatDelaySeconds() = clamp(preferences.getInt(DELAY, 5), 1, 60)
    fun getMaximumRepeatCount() = clamp(preferences.getInt(MAXIMUM, 1), 0, 10)
    fun isRidingModeEnabled() = preferences.getBoolean("riding_mode_enabled", false)

    fun setRidingModeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("riding_mode_enabled", enabled).apply()
    }

    fun save(initial: Boolean, repeat: Boolean, delay: Int, maximum: Int) {
        preferences.edit()
            .putBoolean(INITIAL, initial)
            .putBoolean(REPEAT, repeat)
            .putInt(DELAY, clamp(delay, 1, 60))
            .putInt(MAXIMUM, clamp(maximum, 0, 10))
            .apply()
    }

    private fun clamp(value: Int, minimum: Int, maximum: Int) =
        maxOf(minimum, minOf(maximum, value))

    private companion object {
        const val FILE = "announcement_settings"
        const val INITIAL = "initial"
        const val REPEAT = "repeat"
        const val DELAY = "delay"
        const val MAXIMUM = "maximum"
    }
}
