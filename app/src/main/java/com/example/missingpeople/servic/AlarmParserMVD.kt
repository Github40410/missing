package com.example.missingpeople.servic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.missingpeople.view.MainActivity
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class AlarmParserMVD:BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val parserWork = OneTimeWorkRequestBuilder<ParserWorker>().setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        ).build()
        WorkManager.getInstance(context).enqueueUniqueWork("ParserWorkMVD", ExistingWorkPolicy.REPLACE, parserWork)

        // Переустанавливаем будильник для следующего срабатывания

        setAlarm(context)
    }

    companion object {
        fun setAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmParserMVD::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)


            val thisDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime.now().hour
            } else {
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            }
            val calendar: Calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, thisDate)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if(before(Calendar.getInstance()))
                    add(Calendar.HOUR_OF_DAY, 1)
                }

            // Для Android 6+ (Marshmallow)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
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