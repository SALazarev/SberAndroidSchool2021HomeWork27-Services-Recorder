package com.salazarev.hw27servicesrecorder

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PlayService: Service() {
    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "CHANNEL_ID_1"
        private const val ACTION_PLAY = "ACTION_PLAY"
        private const val ACTION_PAUSE = "ACTION_PAUSE"
    }

    private var isPlay = false
    private var isPause = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Название"
            val description = "Описание"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(remoteViews: RemoteViews): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
            .setContent(remoteViews)
        return builder.build()
    }

    private fun getRemoteViews(): RemoteViews {
        val stopIntent = Intent(this, RecordService::class.java)
        stopIntent.action = ACTION_STOP_SERVICE
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0)

        val playIntent = Intent(this, RecordService::class.java)
        playIntent.action = ACTION_PLAY
        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, 0)

        val remoteViews = RemoteViews(packageName, R.layout.notification)
        remoteViews.setTextViewText(R.id.tv_chronometer, "4:51")
        remoteViews.setTextViewText(R.id.tv_type_work, "${getString(R.string.play)}:")
        remoteViews.setOnClickPendingIntent(R.id.btn_play, playPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.btn_stop, stopPendingIntent)

        return remoteViews
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_PLAY -> {
                if (!isPlay) {
                    Toast.makeText(this, "START", Toast.LENGTH_SHORT).show()
                    updateNotification(createNotification(getRemoteViews()))
                    isPlay = true
                    isPause = false
                }
            }
            ACTION_PAUSE -> {
                if (!isPause && isPlay) {
                    Toast.makeText(this, "PAUSE", Toast.LENGTH_SHORT).show()
                    updateNotification(createNotification(getRemoteViews()))
                    isPlay = false
                    isPause = true
                }

            }
            ACTION_START_SERVICE -> startForeground(
                NOTIFICATION_ID, createNotification(getRemoteViews())
            )
            ACTION_STOP_SERVICE -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(notification: Notification) {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}