package com.salazarev.hw27servicesrecorder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salazarev.hw27servicesrecorder.databinding.ActivityMainBinding
import com.salazarev.hw27servicesrecorder.rv.RecordItem
import com.salazarev.hw27servicesrecorder.rv.RecordListener
import com.salazarev.hw27servicesrecorder.rv.RecordsAdapter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: RecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setToolbar()


        adapter = RecordsAdapter(listOf(RecordItem("Король и шут", "4:51")),
            object : RecordListener {
                override fun onclick(id: String) {
                   startPlayService()
                }
            })
        setRecyclerView(adapter)
    }

    private fun setToolbar() {
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_toolbar)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.btn_record -> {
                        startRecordService()
                        true
                    }
                    else -> super.onOptionsItemSelected(it)
                }
            }
        }
    }

    private fun setRecyclerView(adapter: RecordsAdapter) {
        binding.rvRecords.adapter = adapter
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.rvRecords.layoutManager = layoutManager
        binding.rvRecords.addItemDecoration(
            DividerItemDecoration(
                this,
                layoutManager.orientation
            )
        )
    }

    private fun startRecordService() {
        val intent = Intent(this, RecordService::class.java)
        intent.action = RecordService.ACTION_START_SERVICE
        startService(intent)
    }

    private fun startPlayService() {
        val intent = Intent(this, PlayService::class.java)
        intent.action = PlayService.ACTION_START_SERVICE
        startService(intent)
    }
}