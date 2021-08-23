package com.salazarev.hw27servicesrecorder.play

import android.media.MediaPlayer
import java.io.File

class PlayerRecord(dir: String) {
    var playStatus = PlayState.PLAY
    val fileName: String = File(dir).name
    var playTime: Long = 0

    private val mediaPlayer: MediaPlayer = MediaPlayer().apply {
        setDataSource(dir)
        prepare()
    }

    fun setFinishRecordCallback(function: () -> Unit) {
        mediaPlayer.setOnCompletionListener { function.invoke() }
    }

    enum class PlayState {
        PLAY,
        PAUSE,
        STOP
    }

    fun pause(){
        mediaPlayer.pause()
    }

    fun stop(){
        playTime = 0
        mediaPlayer.stop()
        mediaPlayer.prepare()
        mediaPlayer.seekTo(0)
    }
    fun play(){
        mediaPlayer.start()
        playStatus = PlayState.PLAY
    }

}