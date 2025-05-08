package com.example.missingpeople.servic

import android.content.Context
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.repositor.RepWebMVD
import com.example.missingpeople.repositor.RussianRegion
import com.example.missingpeople.view.NotificationPeopleMissing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.lang.Exception

class ParserWorker(context: Context, params: WorkerParameters):
    Worker(context, params) {
    override fun doWork(): Result {
        return try {
            val regions = inputData.getStringArray("regions")?.toList() ?: listOf(RussianRegion.ALL.srcName)
            parserMVD(applicationContext, regions)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun parserMVD(context: Context, regions: List<String>) {
        val parserMVD = ParserMVD()
        val reposistMVD = RepWebMVD()
        var people: List<MissingPerson> = emptyList()
        val them = context.getSharedPreferences("app_settings", MODE_PRIVATE).getBoolean("app_theme", false)

        runBlocking(Dispatchers.IO) {
            people = regions.flatMap { regionUrl ->
                val url = if (regionUrl == RussianRegion.ALL.srcName) {
                    reposistMVD.getUrlMVD()
                } else {
                    regionUrl
                }
                parserMVD.parserPersonMissing(
                    parserMVD.collectUniqueLinks(parserMVD.extractAllPageUrls(url)),
                    context,
                    them
                )
            }
        }

        if (people.isNotEmpty()) {
            android.os.Handler(Looper.getMainLooper()).post {
                NotificationPeopleMissing(context).showNotification(people[1])
            }
        }
    }
}