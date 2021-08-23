package com.salazarev.hw27servicesrecorder.play

interface PlayListener {
    fun isPlay(isPlay: PlayService.Companion.PlayState, fileName: String)
}
