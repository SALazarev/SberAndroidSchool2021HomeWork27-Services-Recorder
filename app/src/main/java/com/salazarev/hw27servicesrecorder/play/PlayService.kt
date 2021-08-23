package com.salazarev.hw27servicesrecorder.play

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salazarev.hw27servicesrecorder.R
import java.io.File
import java.util.concurrent.TimeUnit

class PlayService : Service() {
    companion object {
        enum class PlayState {
            PLAY,
            PAUSE,
            STOP
        }

        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "CHANNEL_ID_1"
        private const val ACTION_PLAY = "ACTION_PLAY"
    }

    private lateinit var dir: String
    private lateinit var fileName: String

    private lateinit var playListener: PlayListener
    private val binder = LocalPlayServiceBinder()

    private var playStatus = PlayState.PLAY

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val timerTaskRunnable: Runnable
    private var playTime: Long = 0
    private lateinit var notificationManager: NotificationManagerCompat

    private lateinit var mediaPlayer: MediaPlayer

    init {
        timerTaskRunnable = object : Runnable {
            override fun run() {
                if (playStatus == PlayState.PAUSE) {
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

    fun setListener(playListener: PlayListener) {
        this.playListener = playListener
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        checkIntent(intent)
        dir = intent.getStringExtra("dir").toString()
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        checkIntent(intent)
        return START_NOT_STICKY
    }

    private fun checkIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PLAY -> {
                if (playStatus == PlayState.PLAY) pause()
                else if (playStatus == PlayState.PAUSE) play()
                updateNotification(createNotification(getRemoteViews(getTime(playTime))))
            }
            ACTION_STOP_SERVICE -> {
                stop()
            }
        }
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

        val imageId = if (playStatus == PlayState.PLAY) R.drawable.outline_pause_white_48
        else R.drawable.outline_play_arrow_white_48
        remoteViews.setImageViewResource(R.id.btn_play_status, imageId)
        return remoteViews
    }

    private fun play() {
        startTimerTask()
        mediaPlayer.start()
        playStatus = PlayState.PLAY
        playListener.isPlay(playStatus, fileName)
    }

    fun startMyService() {
        startForeground(
            NOTIFICATION_ID, createNotification(getRemoteViews(getTime(playTime)))
        )
        fileName = File(dir).name
        setUpPlayer(dir)
        play()
        playListener.isPlay(playStatus, fileName)
        startTimerTask()
    }

    private fun pause() {
        stopTimerTask()
        mediaPlayer.pause()
        playStatus = PlayState.PAUSE
        playListener.isPlay(playStatus, fileName)
    }

    private fun setUpPlayer(dir: String) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(dir)
            setOnCompletionListener {
                this@PlayService.stop()
            }
            prepare()
        }
    }

    private fun stop() {
        playTime = 0
        stopTimerTask()
        mediaPlayer.stop()
        mediaPlayer.prepare()
        mediaPlayer.seekTo(0)
        playStatus = PlayState.STOP
        playListener.isPlay(playStatus, fileName)
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


    private fun getTime(millis: Long): String = String.format(
        "%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
        TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
    )

    override fun onDestroy() {
        super.onDestroy()
        if (playStatus == PlayState.PAUSE || playStatus == PlayState.PLAY) stop()
    }

    inner class LocalPlayServiceBinder : Binder() {
        fun getService(): PlayService = this@PlayService
    }
}