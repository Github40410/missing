package com.example.missingpeople.servic

import android.content.Context
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.missingpeople.R
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.repositor.RepWebMVD
import com.example.missingpeople.view.NotificationPeopleMissing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.internal.http2.Http2Reader
import java.lang.Exception
import java.util.logging.Handler

class ParserWorker(context: Context, params: WorkerParameters):
    Worker(context, params) {
    override fun doWork(): Result {
        return try {
            parserMVD(applicationContext)
            Result.success()
        }catch (e: Exception){
            Result.retry()
        }
    }

    override fun getForegroundInfo(): ForegroundInfo {
        return super.getForegroundInfo()
    }

    private fun parserMVD(context: Context){
        val parserMVD: ParserMVD = ParserMVD()
        val reposistMVD = RepWebMVD()
        val urlMVD = reposistMVD.getUrlMVD()
        val listURL: ArrayList<String> = ArrayList<String>()
        listURL.add(urlMVD)
        var people: List<MissingPerson>

        runBlocking(Dispatchers.IO) {
            people = parserMVD.parserPersonMissing(parserMVD.collectUniqueLinks(parserMVD.extractAllPageUrls(urlMVD)), context)
        }
        android.os.Handler(Looper.getMainLooper()).post{
            Toast.makeText(
                context,
                "Фоновый процесс функционирует",
                Toast.LENGTH_SHORT
            ).show()
        }

        if(people.isNotEmpty()) {
            android.os.Handler(Looper.getMainLooper()).post {
                NotificationPeopleMissing(context).showNotification(people[0])
            }
        }
    }
}