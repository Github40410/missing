package com.example.missingpeople.servic

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.repositor.MissingPersonDatabase
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
        val dbHelper = MissingPersonDatabase(context)
        val theme = context.getSharedPreferences("app_settings", MODE_PRIVATE).getBoolean("app_theme", false)

        runBlocking(Dispatchers.IO) {
            // 1. Получаем все сохраненные URL из базы
            val existingUrls = dbHelper.getAllUrls().toSet()
            val newPeople = mutableListOf<MissingPerson>()

            regions.forEach { regionUrl ->
                val baseUrl = if (regionUrl == RussianRegion.ALL.srcName) {
                    reposistMVD.getUrlMVD()
                } else {
                    regionUrl
                }

                // 2. Получаем все URL страниц с пропавшими
                val pageUrls = parserMVD.extractAllPageUrls(baseUrl)
                // 3. Получаем уникальные URL персон
                val personUrls = parserMVD.collectUniqueLinks(pageUrls)

                // 4. Фильтруем только новые URL
                val newUrls = personUrls.filterNot { existingUrls.contains(it) }

                if (newUrls.isNotEmpty()) {
                    // 5. Добавляем новые URL в базу
                    dbHelper.addMissingPersonUrls(newUrls)

                    // 6. Парсим информацию о новых пропавших
                    val people = parserMVD.parserPersonMissing(newUrls, context, theme)
                    newPeople.addAll(people)

                    // 7. Сохраняем новых людей в основную таблицу
                    people.forEach { person ->
                        dbHelper.addMissingPerson(person, context)
                    }
                }
            }

            // 8. Показываем уведомление о новых пропавших
            if (newPeople.isNotEmpty()) {
                showNotifications(context, newPeople)
            }
        }
    }

    private fun showNotifications(context: Context, people: List<MissingPerson>) {
        Handler(Looper.getMainLooper()).post {
            val notificationManager = NotificationPeopleMissing(context)

            // Показываем уведомление для каждого нового человека
            people.forEachIndexed { index, person ->
                if (index < 3) { // Ограничим количество уведомлений
                    notificationManager.showNotification(person)
                }
            }

            // Общее уведомление о количестве новых записей
            if (people.size > 1) {
                Toast.makeText(
                    context,
                    "Обнаружено ${people.size} новых пропавших",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}