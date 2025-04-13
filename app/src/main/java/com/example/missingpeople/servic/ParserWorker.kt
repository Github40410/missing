package com.example.missingpeople.servic

import android.content.Context
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.missingpeople.R
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.repositor.RepWebMVD
import com.example.missingpeople.view.NotificationPeopleMissing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class ParserWorker(context: Context, params: WorkerParameters):
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            parserMVD(applicationContext)
            Result.success()
        }catch (e: Exception){
            Result.retry()
        }
    }

    private suspend fun parserMVD(context: Context){
        val parserMVD: ParserMVD = ParserMVD()
        val reposistMVD = RepWebMVD()
        val urlMVD = reposistMVD.getUrlMVD()
        val listURL: ArrayList<String> = ArrayList<String>()
        listURL.add(urlMVD)
        var people: List<MissingPerson>

        withContext(Dispatchers.IO) {
            people = parserMVD.parserPersonMissing(listURL, context)
        }
        withContext(Dispatchers.Main){
            Toast.makeText(
                context,
                "Фоновый процесс функционирует",
                Toast.LENGTH_SHORT
            ).show()
        }
        if(people.isNotEmpty()){
            withContext(Dispatchers.Main) {
                NotificationPeopleMissing(context).showNotification(people.get(1))
            }

        }
    }
}