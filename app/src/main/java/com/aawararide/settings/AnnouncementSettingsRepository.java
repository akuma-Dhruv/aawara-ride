package com.aawararide.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class AnnouncementSettingsRepository {
    private static final String FILE = "announcement_settings";
    private static final String INITIAL = "initial";
    private static final String REPEAT = "repeat";
    private static final String DELAY = "delay";
    private static final String MAXIMUM = "maximum";

    private final SharedPreferences preferences;

    public AnnouncementSettingsRepository(Context context) {
        preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean isInitialEnabled() {
        return preferences.getBoolean(INITIAL, true);
    }

    public boolean isRepeatEnabled() {
        return preferences.getBoolean(REPEAT, true);
    }

    public int getRepeatDelaySeconds() {
        return clamp(preferences.getInt(DELAY, 5), 1, 60);
    }

    public int getMaximumRepeatCount() {
        return clamp(preferences.getInt(MAXIMUM, 1), 0, 10);
    }

    public boolean isRidingModeEnabled() {
        return preferences.getBoolean("riding_mode_enabled", false);
    }

    public void setRidingModeEnabled(boolean enabled) {
        preferences.edit().putBoolean("riding_mode_enabled", enabled).apply();
    }

    public void save(boolean initial, boolean repeat, int delay, int maximum) {
        preferences.edit()
                .putBoolean(INITIAL, initial)
                .putBoolean(REPEAT, repeat)
                .putInt(DELAY, clamp(delay, 1, 60))
                .putInt(MAXIMUM, clamp(maximum, 0, 10))
                .apply();
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
