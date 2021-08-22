package com.salazarev.hw27servicesrecorder

import android.os.Environment
import androidx.lifecycle.ViewModel
import com.salazarev.hw27servicesrecorder.rv.RecordItem
import java.io.File

class MainViewModel : ViewModel() {

    private val dir = "${Environment.getExternalStorageDirectory().absolutePath}/Record"

    fun getRecordItems(): List<RecordItem> =
        File(dir).listFiles()?.filter { it.isFile }?.map { RecordItem(it.name) } ?: emptyList()
}