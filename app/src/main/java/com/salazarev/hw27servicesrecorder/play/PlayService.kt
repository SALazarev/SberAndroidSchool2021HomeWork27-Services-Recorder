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
        enum class PlayState() {
            PLAY,
            PAUSE,
            STOP
        }

        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "CHANNEL_ID_1"
        private const val ACTION_PLAY = "ACTION_PLAY"
    }

    lateinit var fileName: String

    private lateinit var playListener: PlayListener


    private val binder = LocalPlayServiceBinder()

    private var playStatus = PlayState.PLAY

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val timerTaskRunnable: Runnable
    private var playTime: Long = 0
    lateinit var notificationManager: NotificationManagerCompat

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

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Название"
            val description = "Описание"
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
            if (playStatus == PlayState.PLAY) R.drawable.outline_pause_white_48 else R.drawable.outline_play_arrow_white_48
        remoteViews.setImageViewResource(R.id.btn_play_status, imageId)
        return remoteViews
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        checkIntent(intent)
        return START_NOT_STICKY
    }

    private fun checkIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PLAY -> {
                if (playStatus == PlayState.PLAY) {
                    pause()
                    stopTimerTask()
                    playListener.isPlay(PlayState.PAUSE, fileName)
                } else {
                    if (playStatus == PlayState.PAUSE) play()
                    startTimerTask()
                }
                startTimerTask()
                updateNotification(createNotification(getRemoteViews(getTime(playTime))))

            }
            ACTION_STOP_SERVICE -> {
                notificationManager.cancel(NOTIFICATION_ID)
                playTime = 0
                stopPlay()
                stopTimerTask()
                playListener.isPlay(PlayState.STOP, fileName)
            }
        }
    }

    fun startMyService() {
        startForeground(
            NOTIFICATION_ID, createNotification(getRemoteViews(getTime(playTime)))
        )
        fileName = File(dir).name
        setUpPlayer(dir)
        play()
        playListener.isPlay(PlayState.PLAY, fileName)
        startTimerTask()
    }

    private fun pause() {
        mediaPlayer.pause()
        playStatus = PlayState.PAUSE
    }

    lateinit var mediaPlayer: MediaPlayer

    private fun setUpPlayer(dir: String) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(dir)
            setOnCompletionListener {
                stopPlay()
                playListener.isPlay(PlayState.PAUSE, fileName)
            }
            prepare()
        }
    }

    private fun play() {
        mediaPlayer.start()
        playStatus = PlayState.PLAY
    }

    private fun stopPlay() {
        mediaPlayer.stop()
        mediaPlayer.prepare()
        mediaPlayer.seekTo(0)
        playStatus = PlayState.PAUSE
        stopSelf()
    }


    private fun updateNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
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
        if (mediaPlayer.isPlaying) {
            stopPlay()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("TAG", "unbind")
        notificationManager.cancel(NOTIFICATION_ID)
        if (playStatus == PlayState.PLAY) {
            playTime = 0
            stopPlay()
            stopTimerTask()
            playListener.isPlay(PlayState.STOP, fileName)
        }
        return super.onUnbind(intent)
    }

    lateinit var dir: String

    override fun onBind(intent: Intent): IBinder {
        Log.d("TAG", "onBind")
        checkIntent(intent)
        dir = intent.getStringExtra("dir").toString()
        return binder
    }

    inner class LocalPlayServiceBinder : Binder() {
        fun getService(): PlayService = this@PlayService
    }


    fun setListener(playListener: PlayListener) {
        this.playListener = playListener
    }
}