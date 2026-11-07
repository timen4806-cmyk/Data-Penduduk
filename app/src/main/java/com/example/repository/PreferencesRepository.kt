package com.example.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "datawarga_preferences")

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val themeMode: AppThemeMode,
    val isDailyReminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val isAutoCloudSyncEnabled: Boolean,
    val cloudSyncIntervalMinutes: Int,
    val lastSyncTimestamp: Long
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val AUTO_CLOUD_SYNC_ENABLED = booleanPreferencesKey("auto_cloud_sync_enabled")
        val CLOUD_SYNC_INTERVAL = intPreferencesKey("cloud_sync_interval")
        val LAST_SYNC_TIMESTAMP = stringPreferencesKey("last_sync_timestamp")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
        val themeMode = try {
            AppThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }

        UserPreferences(
            themeMode = themeMode,
            isDailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 9,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            isAutoCloudSyncEnabled = prefs[Keys.AUTO_CLOUD_SYNC_ENABLED] ?: true,
            cloudSyncIntervalMinutes = prefs[Keys.CLOUD_SYNC_INTERVAL] ?: 60,
            lastSyncTimestamp = (prefs[Keys.LAST_SYNC_TIMESTAMP] ?: "0").toLongOrNull() ?: 0L
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_REMINDER_ENABLED] = enabled
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setAutoCloudSync(enabled: Boolean, intervalMinutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CLOUD_SYNC_ENABLED] = enabled
            prefs[Keys.CLOUD_SYNC_INTERVAL] = intervalMinutes
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = timestamp.toString()
        }
    }
}
