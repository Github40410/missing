package com.example.missingpeople.view

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.

                }
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }catch (e: Exception) {
                // Fallback notification if image loading fails
                val fallbackNotification = createFallbackNotification(person)
                NotificationManagerCompat.from(context).notify(notificationId, fallbackNotification)
            }

        }
    }

    private fun createNotification(person: MissingPerson, bitmap: Bitmap): Notification {
        // Create custom notification layout
        val contentView = RemoteViews(context.packageName, R.layout.notification_missing_person)
        contentView.setTextViewText(R.id.tvName, person.name)
        contentView.setImageViewBitmap(R.id.ivPhoto, bitmap)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.error_image)
            .setCustomContentView(contentView)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }



}


