package com.salazarev.hw27servicesrecorder.view.rv

import com.salazarev.hw27servicesrecorder.play.AudioPlayer

class RecordItem(
    val name: String,
    var playStatus: AudioPlayer.PlayState = AudioPlayer.PlayState.STOP
)