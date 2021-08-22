package com.salazarev.hw27servicesrecorder.rv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.salazarev.hw27servicesrecorder.R
import java.io.File

class RecordsAdapter(
    private var data: List<RecordItem>,
    private val listener: RecordListener
): RecyclerView.Adapter<RecordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
       holder.bindView(data[position]) {listener.onclick(data[position].name)}
    }

    override fun getItemCount(): Int {
      return data.size
    }

    fun updateData(data: List<RecordItem>){
        this.data = data
        notifyDataSetChanged()
    }

    fun updateData(rootFile: File) {
        this.data = listFiles(rootFile)
        notifyDataSetChanged()
    }

    private fun listFiles(rootFile: File): List<RecordItem> =
        rootFile.listFiles()?.map { RecordItem(it.absolutePath) } ?: emptyList()

}