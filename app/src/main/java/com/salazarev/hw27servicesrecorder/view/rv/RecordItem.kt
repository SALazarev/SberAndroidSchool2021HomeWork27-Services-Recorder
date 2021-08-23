package com.salazarev.hw27servicesrecorder.view.rv

import com.salazarev.hw27servicesrecorder.play.PlayService

class RecordItem(
    val name: String,
    var playStatus: PlayService.Companion.PlayState = PlayService.Companion.PlayState.STOP
)