package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.notification.NotificationHelper
import com.example.repository.AuthRepository
import com.example.repository.ExportRepository
import com.example.repository.PendudukRepository
import com.example.repository.PreferencesRepository
import com.example.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DataWargaApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val pendudukRepository by lazy { PendudukRepository(database.pendudukDao()) }
    val authRepository by lazy { AuthRepository(database.userDao()) }
    val preferencesRepository by lazy { PreferencesRepository(this) }
    val syncRepository by lazy { SyncRepository(this, database.pendudukDao(), preferencesRepository) }
    val exportRepository by lazy { ExportRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = preferencesRepository.preferencesFlow.first()
                if (prefs.isDailyReminderEnabled) {
                    NotificationHelper.scheduleDailyReminder(
                        this@DataWargaApp,
                        prefs.reminderHour,
                        prefs.reminderMinute
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}
