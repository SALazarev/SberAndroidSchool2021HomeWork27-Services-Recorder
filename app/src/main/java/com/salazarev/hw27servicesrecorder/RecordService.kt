package com.salazarev.hw27servicesrecorder

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class RecordService : Service() {
    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "CHANNEL_ID_1"
        private const val ACTION_PLAY = "ACTION_PLAY"
    }

    private var isPlay = true

    private val idImagePlay = R.drawable.outline_play_arrow_white_48
    private val idImagePause = R.drawable.outline_pause_white_48

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.recorder)
            val description = getString(R.string.record_sound)
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

    private fun getRemoteViews(time: String): RemoteViews {
        val stopIntent = Intent(this, RecordService::class.java)
        stopIntent.action = ACTION_STOP_SERVICE
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0)

        val playIntent = Intent(this, RecordService::class.java)
        playIntent.action = ACTION_PLAY
        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, 0)

        val remoteViews = RemoteViews(packageName, R.layout.notification)
        remoteViews.setTextViewText(R.id.tv_chronometer, time)
        remoteViews.setTextViewText(R.id.tv_type_work, "${getString(R.string.record)}:")
        remoteViews.setOnClickPendingIntent(R.id.btn_play, playPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.btn_stop, stopPendingIntent)

        return remoteViews
    }

    private fun playStatus(remoteViews: RemoteViews): RemoteViews {
        val idImageStatus = if (isPlay) idImagePause else idImagePlay
        remoteViews.setImageViewResource(R.id.btn_play, idImageStatus)
        return remoteViews
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_PLAY -> {
                updateNotification(createNotification(playStatus(getRemoteViews("4:51"))))
                isPlay = !isPlay
                if (isPlay){
                    record()
                }
                else pauseRecord()
            }
            ACTION_START_SERVICE -> {
                startForeground(
                    NOTIFICATION_ID, createNotification(getRemoteViews("4:51"))
                )
                record()
            }
            ACTION_STOP_SERVICE -> {
                stopRecord()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopRecord() {
            mediaRecorder?.stop()
        Toast.makeText(this, "FILE IS SAVE", Toast.LENGTH_SHORT).show()
    }

    private fun pauseRecord() {

    }

    private var mediaRecorder: MediaRecorder? = null

    private fun record() {
        mediaRecorder?.release();
        mediaRecorder = null;
        val fileName =
            "${Environment.getExternalStorageDirectory()}/${SystemClock.elapsedRealtime()}.wav"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(fileName)
            prepare()
            start()
        }

    }

    private fun updateNotification(notification: Notification) {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "DESTROY RECORD SERVICE", Toast.LENGTH_SHORT).show()
    }
}