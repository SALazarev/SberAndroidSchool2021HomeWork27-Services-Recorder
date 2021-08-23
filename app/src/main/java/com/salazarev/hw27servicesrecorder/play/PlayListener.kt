package com.salazarev.hw27servicesrecorder.play

interface PlayListener {
    fun isPlay(playStatus: AudioPlayer.PlayState, fileName: String)
}
