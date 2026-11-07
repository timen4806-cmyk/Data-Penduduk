package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefsRepo = PreferencesRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = prefsRepo.preferencesFlow.first()
            if (prefs.isDailyReminderEnabled) {
                NotificationHelper.showReminderNotification(
                    context,
                    "Pengingat Administrasi DataWarga",
                    "Jangan lupa periksa dan mutakhirkan data penduduk harian serta lakukan pencadangan data."
                )

                // Reschedule for next day
                NotificationHelper.scheduleDailyReminder(
                    context,
                    prefs.reminderHour,
                    prefs.reminderMinute
                )
            }
        }
    }
}
