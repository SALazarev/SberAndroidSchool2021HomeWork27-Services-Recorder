package com.salazarev.hw27servicesrecorder.play

interface PlayListener {
    fun isPlay(playStatus: PlayerRecord.PlayState, fileName: String)
}
