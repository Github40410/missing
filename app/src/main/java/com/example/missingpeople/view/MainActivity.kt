package com.example.missingpeople.view

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginEnd
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.missingpeople.R
import com.example.missingpeople.databinding.ActivityMainBinding
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.repositor.RepWebMVD
import com.example.missingpeople.servic.AlarmParserMVD
import com.example.missingpeople.servic.ConstructView
import com.example.missingpeople.servic.ParserMVD
import com.example.missingpeople.servic.ParserWorker
import com.example.missingpeople.servic.WorkScheduler
import com.google.android.material.internal.ViewUtils.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repositMVD: RepWebMVD = RepWebMVD()
    private val parserMVD: ParserMVD = ParserMVD()
    private lateinit var linearLayoutPeople: LinearLayout // Используем lateinit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)


        // Настройка системных отступов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkExactAlarmPermission()

        // Запуск периодической работы
        AlarmParserMVD.setAlarm(this)

        val urlMVD = repositMVD.getUrlMVD()

        lifecycleScope.launch(Dispatchers.IO){
            // Запускаем в IO для сетевых/тяжелых операций
            val constructView = ConstructView(findViewById(R.id.linearLayoutAllPeople))
            parserMVD.constructView = constructView
            val people = parserMVD.parserPersonMissing(parserMVD.collectUniqueLinks(parserMVD.extractAllPageUrls(urlMVD)), this@MainActivity)

            // Обновляем UI в главном потоке
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "Найдено ссылок: ${people.size}",
                    Toast.LENGTH_SHORT
                ).show()

                showNotification(people.get(0))
            }
        }
    }

    private fun showNotification(person: MissingPerson) {
        NotificationPeopleMissing(this).showNotification(person)
    }


    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Открываем настройки, если разрешения нет
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

}