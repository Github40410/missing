package com.example.missingpeople.view

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.RemoteViews
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.missingpeople.R
import com.example.missingpeople.repositor.MissingPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.discardingSink
import java.net.URL

class NotificationPeopleMissing(private val context: Context) {
    private val channelId = "missing_persons_channel"
    private val notificationId = System.currentTimeMillis().toInt()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val name = "Пропавшие люди"
            val description = "Уведомление о пропавших без вести"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channal = NotificationChannel(channelId, name, importance).apply{
                this.description = description
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channal)
        }
    }

    private fun createFallbackNotification(percon: MissingPerson):Notification{

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.error_image)
            .setContentTitle(percon.name)
            .setContentText(percon.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    fun showNotification(person: MissingPerson){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(person.photos)
                    .submit(300, 300)
                    .get()

                val notification = createNotification(person, bitmap)
                showNotificationWithPermissionCheck(notification, person)
            } catch (e: Exception) {
                val fallbackNotification = createFallbackNotification(person)
                showNotificationWithPermissionCheck(fallbackNotification, person)
            }
        }
    }


    private fun showNotificationWithPermissionCheck(notification: Notification, person: MissingPerson) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun createNotification(person: MissingPerson, bitmap: Bitmap): Notification {
        // Создаем Intent для открытия PersonDetailActivity
        val intent = Intent(context, PersonDetailActivity::class.java).apply {
            putExtra(PersonDetailActivity.EXTRA_PERSON, person)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Создаем PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Создаем кастомный layout для уведомления
        val contentView = RemoteViews(context.packageName, R.layout.notification_missing_person)
        contentView.setTextViewText(R.id.tvName, person.name)
        contentView.setImageViewBitmap(R.id.ivPhoto, bitmap)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.error_image)
            .setCustomContentView(contentView)
            .setContentIntent(pendingIntent) // Устанавливаем PendingIntent
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Уведомление исчезнет при клике
            .build()
    }


}


