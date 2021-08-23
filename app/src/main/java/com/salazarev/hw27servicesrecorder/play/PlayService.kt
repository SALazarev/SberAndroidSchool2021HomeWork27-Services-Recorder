package com.salazarev.hw27servicesrecorder.play

import android.app.*
import android.content.Intent
import android.os.*
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salazarev.hw27servicesrecorder.R
import java.util.concurrent.TimeUnit

class PlayService : Service() {
    companion object {
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        const val DIRECTORY_KEY = "dir"
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "CHANNEL_ID_1"
        private const val ACTION_PLAY = "ACTION_PLAY"
    }

    private lateinit var playListener: PlayListener
    private val binder = LocalPlayServiceBinder()

    var playTime: Long = 0

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val timerTaskRunnable: Runnable
    private lateinit var notificationManager: NotificationManagerCompat

    init {
        timerTaskRunnable = object : Runnable {
            override fun run() {
                if (audioPlayer.playStatus == AudioPlayer.PlayState.PAUSE) {
                    handler.removeCallbacks(this)
                } else {
                    playTime += 1000
                    handler.postDelayed(this, 1000)
                    updateNotification(
                        createNotification(
                            getRemoteViews(getTime(playTime))
                        )
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        val dir = intent.getStringExtra(DIRECTORY_KEY).toString()
        audioPlayer = AudioPlayer(dir)
        audioPlayer.setFinishRecordCallback { this@PlayService.stop() }
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        checkIntent(intent)
        return START_NOT_STICKY
    }

    private fun checkIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PLAY -> {
                if (audioPlayer.playStatus == AudioPlayer.PlayState.PLAY) pause()
                else if (audioPlayer.playStatus == AudioPlayer.PlayState.PAUSE) play()
                updateNotification(createNotification(getRemoteViews(getTime(playTime))))
            }
            ACTION_STOP_SERVICE -> {
                stop()
            }
        }
    }

    fun setListener(playListener: PlayListener) {
        this.playListener = playListener
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.pleer)
            val description = getString(R.string.play_audio)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
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
        val stopIntent = Intent(this, PlayService::class.java)
        stopIntent.action = ACTION_STOP_SERVICE
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0)

        val playIntent = Intent(this, PlayService::class.java)
        playIntent.action = ACTION_PLAY
        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, 0)

        val remoteViews = RemoteViews(packageName, R.layout.notification)
        remoteViews.setTextViewText(R.id.tv_chronometer, time)
        remoteViews.setTextViewText(R.id.tv_type_work, "${getString(R.string.play)}:")
        remoteViews.setOnClickPendingIntent(R.id.btn_play_status, playPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.btn_stop, stopPendingIntent)

        val imageId =
            if (audioPlayer.playStatus == AudioPlayer.PlayState.PLAY) R.drawable.outline_pause_white_48
            else R.drawable.outline_play_arrow_white_48
        remoteViews.setImageViewResource(R.id.btn_play_status, imageId)
        return remoteViews
    }

    private fun getTime(millis: Long): String = String.format(
        "%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
        TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
    )



    private fun play() {
        startTimerTask()
        audioPlayer.play()
        playListener.isPlay(audioPlayer.playStatus, audioPlayer.fileName)
    }

    private lateinit var audioPlayer: AudioPlayer
    fun startMyService() {
        startForeground(
            NOTIFICATION_ID, createNotification(getRemoteViews(getTime(playTime)))
        )
        play()
    }

    private fun pause() {
        audioPlayer.pause()
        stopTimerTask()
        playListener.isPlay(audioPlayer.playStatus, audioPlayer.fileName)
    }


    private fun stop() {
        playTime = 0
        stopTimerTask()
        audioPlayer.stop()
        playListener.isPlay(audioPlayer.playStatus, audioPlayer.fileName)
    }

    private fun updateNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startTimerTask() {
        stopTimerTask()
        handler.postDelayed(timerTaskRunnable, 1000)
    }

    private fun stopTimerTask() {
        handler.removeCallbacks(timerTaskRunnable)
    }


    override fun onDestroy() {
        super.onDestroy()
        if (audioPlayer.playStatus == AudioPlayer.PlayState.PAUSE || audioPlayer.playStatus == AudioPlayer.PlayState.PLAY) stop()
    }

    inner class LocalPlayServiceBinder : Binder() {
        fun getService(): PlayService = this@PlayService
    }
}