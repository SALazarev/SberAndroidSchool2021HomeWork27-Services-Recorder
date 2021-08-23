package com.salazarev.hw27servicesrecorder.record

import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import com.salazarev.hw27servicesrecorder.R
import java.text.SimpleDateFormat
import java.util.*

class Recorder {
    var mediaRecorder: MediaRecorder? = null
    var recordStatus = RecordState.RECORD

    companion object {
        private const val FILE_NAME_FORMAT = "dd_MM_yyyy-HH_mm_ss"
        const val FOLDER_NAME = "ServiceRecorder"
    }

    private val fileDir = "${Environment.getExternalStorageDirectory()}/$FOLDER_NAME"


    enum class RecordState(val imageStatus: Int) {
        RECORD(R.drawable.outline_pause_white_48),
        PAUSE(R.drawable.outline_play_arrow_white_48),
    }

    fun record() {
        mediaRecorder?.release();
        mediaRecorder = null;
        val dirFile = "$fileDir/${generateNameFileByCurrentTime(FILE_NAME_FORMAT)}.wav"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(dirFile)
            prepare()
            start()
        }
    }

    fun replay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (recordStatus == RecordState.PAUSE) mediaRecorder?.resume()
        } else {
            record()
        }
        recordStatus = RecordState.RECORD
    }

    fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (recordStatus == RecordState.RECORD) mediaRecorder?.pause()
        } else {
            stop()
        }
        recordStatus = RecordState.PAUSE
    }

    fun stop() {
        mediaRecorder?.stop()
        recordStatus = RecordState.PAUSE
    }

    fun generateNameFileByCurrentTime(fileNameFormat: String): String {
        val df = SimpleDateFormat(fileNameFormat, Locale.ENGLISH)
        return df.format(Calendar.getInstance().time)
    }
}