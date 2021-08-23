package com.salazarev.hw27servicesrecorder.view.rv

import com.salazarev.hw27servicesrecorder.play.PlayerRecord

class RecordItem(
    val name: String,
    var playStatus: PlayerRecord.PlayState = PlayerRecord.PlayState.STOP
)