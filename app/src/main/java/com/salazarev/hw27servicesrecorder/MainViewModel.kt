package com.salazarev.hw27servicesrecorder

import android.os.Environment
import androidx.lifecycle.ViewModel
import com.salazarev.hw27servicesrecorder.rv.RecordItem
import java.io.File

class MainViewModel : ViewModel() {
     val dir = "${Environment.getExternalStorageDirectory().absolutePath}/${RecordService.FOLDER_NAME}"

    init{
        createFolder()
    }

    fun getRecordItems(): List<RecordItem>{
        createFolder()
       return File(dir).listFiles()?.filter { it.isFile }?.map { RecordItem(it.name) } ?: emptyList()
    }


    private fun createFolder() {
        val folder = File(dir)
        if (!folder.exists())  folder.mkdirs()
    }
}