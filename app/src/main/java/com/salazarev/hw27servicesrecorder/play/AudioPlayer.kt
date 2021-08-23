package com.salazarev.hw27servicesrecorder.play

import android.media.MediaPlayer
import java.io.File

class AudioPlayer(dir: String) {
    var playStatus = PlayState.PLAY
    val fileName: String = File(dir).name


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
        playStatus = PlayState.PAUSE
    }

    fun stop(){
        mediaPlayer.stop()
        mediaPlayer.prepare()
        mediaPlayer.seekTo(0)
        playStatus = PlayState.STOP
    }
    fun play(){
        mediaPlayer.start()
        playStatus = PlayState.PLAY
    }

}