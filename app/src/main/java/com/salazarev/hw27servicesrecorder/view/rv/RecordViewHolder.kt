package com.salazarev.hw27servicesrecorder.view.rv

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.salazarev.hw27servicesrecorder.R
import com.salazarev.hw27servicesrecorder.databinding.ItemRecordBinding
import com.salazarev.hw27servicesrecorder.play.AudioPlayer

class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding = ItemRecordBinding.bind(this.itemView)
    private val nameRecordTv: TextView
    private val playStatusIv: ImageView

    init {
        nameRecordTv = binding.tvRecordName
        playStatusIv = binding.btnPlayStatus
    }

    fun bindView(recordItem: RecordItem, clickListener: View.OnClickListener) {
       itemView.setOnClickListener(clickListener)
        nameRecordTv.text= recordItem.name
        when (recordItem.playStatus){
            AudioPlayer.PlayState.PLAY -> playStatusIv.setImageResource(R.drawable.outline_pause_24)
            AudioPlayer.PlayState.PAUSE -> playStatusIv.setImageResource(R.drawable.outline_play_arrow_24)
            AudioPlayer.PlayState.STOP -> playStatusIv.setImageResource(0)
        }
    }
}
