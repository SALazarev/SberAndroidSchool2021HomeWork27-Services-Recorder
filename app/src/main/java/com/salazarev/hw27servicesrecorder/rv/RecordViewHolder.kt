package com.salazarev.hw27servicesrecorder.rv

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.salazarev.hw27servicesrecorder.databinding.ItemRecordBinding

class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding = ItemRecordBinding.bind(this.itemView)
    private val nameRecordTv: TextView

    init {
        nameRecordTv = binding.tvRecordName
    }

    fun bindView(recordItem: RecordItem, clickListener: View.OnClickListener) {
       itemView.setOnClickListener(clickListener)
        nameRecordTv.text= recordItem.name
    }
}
