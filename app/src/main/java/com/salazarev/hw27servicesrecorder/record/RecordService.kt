package com.salazarev.hw27servicesrecorder.record

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salazarev.hw27servicesrecorder.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class RecordService : Service() {
    companion object {
        enum class RecordState(val imageStatus: Int) {
            RECORD(R.drawable.outline_pause_white_48),
            PAUSE(R.drawable.outline_play_arrow_white_48),
        }

        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "CHANNEL_ID_1"
        private const val ACTION_PLAY = "ACTION_PLAY"

        const val FOLDER_NAME = "ServiceRecorder"
        private const val FILE_NAME_FORMAT = "dd_MM_yyyy-HH_mm_ss"
    }

    private lateinit var recordListener: RecordListener

    private val binder = LocalRecordServiceBinder()

    private var recordStatus = RecordState.RECORD

    private val fileDir = "${Environment.getExternalStorageDirectory()}/$FOLDER_NAME"

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val timerTaskRunnable: Runnable
    private var recordTime: Long = 0
    lateinit var notificationManager: NotificationManagerCompat

    private var mediaRecorder: MediaRecorder? = null

    init {
        timerTaskRunnable = object : Runnable {
            override fun run() {
                if (recordStatus == RecordState.PAUSE) {
                    handler.removeCallbacks(this)
                } else {
                    recordTime += 1000
                    handler.postDelayed(this, 1000)
                    updateNotification(
                        createNotification(
                            getRemoteViews(getTime(recordTime))
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
            val name: CharSequence = getString(R.string.recorder)
            val description = getString(R.string.record_sound)
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
        remoteViews.setImageViewResource(R.id.btn_play_status, recordStatus.imageStatus)
        return remoteViews
    }

    private fun stopRecord() {
        mediaRecorder?.stop()
        recordStatus = RecordState.PAUSE
    }

    private fun pauseRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (recordStatus == RecordState.RECORD) mediaRecorder?.pause()
        } else {
            stopRecord()
        }
        recordStatus = RecordState.PAUSE
    }

    private fun replayRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (recordStatus == RecordState.PAUSE) mediaRecorder?.resume()
        } else {
            record(generateNameFileByCurrentTime(FILE_NAME_FORMAT))
        }
        recordStatus = RecordState.RECORD
    }

    private fun record(fileName: String) {
        mediaRecorder?.release();
        mediaRecorder = null;
        val dirFile = "$fileDir/$fileName.wav"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(dirFile)
            prepare()
            start()
        }
    }

    private fun updateNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("TAG", "DESTROY RECORD SERVICE")
        Toast.makeText(this, "DESTROY RECORD SERVICE", Toast.LENGTH_SHORT).show()
    }


    fun generateNameFileByCurrentTime(fileNameFormat: String): String {
        val df = SimpleDateFormat(fileNameFormat, Locale.ENGLISH)
        return df.format(Calendar.getInstance().time)
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



    inner class LocalRecordServiceBinder : Binder() {
        fun getService(): RecordService = this@RecordService
    }



    fun setListener(recordListener: RecordListener) {
        this.recordListener = recordListener
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("TAG", "onStart")
        checkIntent(intent)
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("TAG", "unbind")
        notificationManager.cancel(NOTIFICATION_ID)
        if (recordStatus == RecordState.RECORD) {
            recordTime = 0
            stopRecord()
            stopTimerTask()
            recordListener.isRecordered()
        }
        return super.onUnbind(intent)
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("TAG", "onBind")
        checkIntent(intent)
        return binder
    }

    fun checkIntent(intent: Intent) {
        Log.d("TAG", "${intent.action}")
        when (intent.action) {
            ACTION_PLAY -> {
                if (recordStatus == RecordState.RECORD) {
                    pauseRecord()
                    stopTimerTask()
                } else {
                    if (recordStatus == RecordState.PAUSE) replayRecord()
                    startTimerTask()
                }
                startTimerTask()
                updateNotification(createNotification(getRemoteViews(getTime(recordTime))))

            }
            ACTION_START_SERVICE -> {
                startForeground(
                    NOTIFICATION_ID, createNotification(getRemoteViews(getTime(recordTime)))
                )
                record(generateNameFileByCurrentTime(FILE_NAME_FORMAT))
                startTimerTask()
            }
            ACTION_STOP_SERVICE -> {
                notificationManager.cancel(NOTIFICATION_ID)
                if (recordStatus == RecordState.RECORD) {
                    recordTime = 0
                    stopRecord()
                    stopTimerTask()
                    recordListener.isRecordered()
                }
            }
        }
    }
}