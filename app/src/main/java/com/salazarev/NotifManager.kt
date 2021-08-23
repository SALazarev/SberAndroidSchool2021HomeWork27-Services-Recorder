package com.salazarev

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.salazarev.hw27servicesrecorder.R
import com.salazarev.hw27servicesrecorder.record.RecordService
import java.util.concurrent.TimeUnit

class NotifManager(context: Context) {
    companion object{
        const val CHANNEL_ID = "CHANNEL_ID_1"

        fun getTimeForNotification(millis: Long): String = String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = context.getString(R.string.recorder)
            val description = context.getString(R.string.record_sound)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            channel.setSound(null, null)
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }



}