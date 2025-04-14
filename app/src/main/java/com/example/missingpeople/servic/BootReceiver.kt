package com.example.missingpeople.servic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val parserWork = PeriodicWorkRequestBuilder<ParserWorker>(15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueue(parserWork)
        }
    }
}