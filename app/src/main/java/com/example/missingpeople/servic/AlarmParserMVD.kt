package com.example.missingpeople.servic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.missingpeople.repositor.RussianRegion
import com.example.missingpeople.view.MainActivity
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class AlarmParserMVD : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = context.getSharedPreferences("app_settings", MODE_PRIVATE)
        val isMonitoringEnabled = sharedPreferences.getBoolean("monitoring_enabled", false)

        if (isMonitoringEnabled) {
            startMonitoringWork(context)
        }

        setAlarm(context)
    }

    private fun startMonitoringWork(context: Context) {
        val regionsJson = context.getSharedPreferences("app_settings", MODE_PRIVATE)
            .getString("monitoring_regions", null)

        regionsJson?.let {
            val type = object : TypeToken<Set<String>>() {}.type
            val regionNames = Gson().fromJson<Set<String>>(it, type)
            val regions = regionNames.mapNotNull { name ->
                RussianRegion.values().find { it.srcName == name }
            }

            val parserWork = OneTimeWorkRequestBuilder<ParserWorker>()
                .setInputData(createInputData(regions))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ParserWorkMVD",
                ExistingWorkPolicy.REPLACE,
                parserWork
            )
        }
    }

    private fun createInputData(regions: List<RussianRegion>): Data {
        return Data.Builder().apply {
            putStringArray("regions", regions.map { it.srcName }.toTypedArray())
        }.build()
    }

    companion object {
        fun setAlarm(context: Context) {
            val sharedPrefs = context.getSharedPreferences("app_settings", MODE_PRIVATE)
            if (!sharedPrefs.getBoolean("monitoring_enabled", false)) {
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmParserMVD::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val triggerAtMillis = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmParserMVD::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
}