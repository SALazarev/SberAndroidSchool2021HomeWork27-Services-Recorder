package com.salazarev.hw27servicesrecorder.record

import android.app.*
import android.content.Intent
import android.os.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salazarev.NotifManager
import com.salazarev.hw27servicesrecorder.R
import java.util.*


class RecordService : Service() {
    companion object {
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        private const val NOTIFICATION_ID = 1
        private const val ACTION_PLAY = "ACTION_PLAY"
    }

    private lateinit var recordListener: RecordListener

    private val binder = LocalRecordServiceBinder()

    private lateinit var recorder: Recorder

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val timerTaskRunnable: Runnable
    private var recordTime: Long = 0
    lateinit var notificationManager: NotificationManagerCompat


    init {
        timerTaskRunnable = object : Runnable {
            override fun run() {
                if (recorder.recordStatus == Recorder.RecordState.PAUSE) {
                    handler.removeCallbacks(this)
                } else {
                    recordTime += 1000
                    handler.postDelayed(this, 1000)
                    updateNotification(
                        createNotification(
                            getRemoteViews(NotifManager.getTimeForNotification(recordTime))
                        )
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
    }

    private fun createNotification(remoteViews: RemoteViews): Notification {
        val builder = NotificationCompat.Builder(this, NotifManager.CHANNEL_ID)
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
        remoteViews.setOnClickPendingIntent(R.id.btn_play_status, playPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.btn_stop, stopPendingIntent)
        remoteViews.setImageViewResource(R.id.btn_play_status, recorder.recordStatus.imageStatus)
        return remoteViews
    }

    private fun stopRecord() {
        recorder.stop()
        recordTime = 0
        stopTimerTask()
        recordListener.isRecordered()
    }

    private fun pauseRecord() {
        recorder.pause()
        stopTimerTask()
    }

    private fun replayRecord() {
        recorder.replay()
        startTimerTask()
    }

    private fun record() {
        recorder.record()
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


    inner class LocalRecordServiceBinder : Binder() {
        fun getService(): RecordService = this@RecordService
    }


    fun setListener(recordListener: RecordListener) {
        this.recordListener = recordListener
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        checkIntent(intent)
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        notificationManager.cancel(NOTIFICATION_ID)
        if (recorder.recordStatus == Recorder.RecordState.RECORD) {
            recordTime = 0
            stopRecord()
            stopTimerTask()
            recordListener.isRecordered()
        }
        return super.onUnbind(intent)
    }

    override fun onBind(intent: Intent): IBinder {
        recorder = Recorder()
        return binder
    }

    fun startMyService() {
        startForeground(
            NOTIFICATION_ID,
            createNotification(getRemoteViews(NotifManager.getTimeForNotification(recordTime)))
        )
        record()
        startTimerTask()
    }

    private fun checkIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PLAY -> {
                if (recorder.recordStatus == Recorder.RecordState.RECORD) pauseRecord()
                else if (recorder.recordStatus == Recorder.RecordState.PAUSE) replayRecord()
                updateNotification(
                    createNotification(
                        getRemoteViews(
                            NotifManager.getTimeForNotification(
                                recordTime
                            )
                        )
                    )
                )

            }
            ACTION_STOP_SERVICE -> stopRecord()
        }
    }
}