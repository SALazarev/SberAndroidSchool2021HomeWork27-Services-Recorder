package com.salazarev.hw27servicesrecorder.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salazarev.hw27servicesrecorder.R
import com.salazarev.hw27servicesrecorder.databinding.ActivityMainBinding
import com.salazarev.hw27servicesrecorder.play.PlayListener
import com.salazarev.hw27servicesrecorder.play.PlayService
import com.salazarev.hw27servicesrecorder.play.AudioPlayer
import com.salazarev.hw27servicesrecorder.record.RecordService
import com.salazarev.hw27servicesrecorder.view.rv.RecordItem
import com.salazarev.hw27servicesrecorder.view.rv.RecordListener
import com.salazarev.hw27servicesrecorder.view.rv.RecordsAdapter


class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_RECORD_CODE = 101
        const val REQUEST_FIND_FILE_CODE = 102
    }

    private lateinit var recordService: RecordService
    private lateinit var playService: PlayService
    private var boundRecordService = false
    private var boundPlayService = false

    private lateinit var viewModel: MainViewModel

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: RecordsAdapter

    private val recordConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            _service: IBinder
        ) {
            val binder = _service as RecordService.LocalRecordServiceBinder
            recordService = binder.getService()
            boundRecordService = true
            recordService.setListener(object :
                com.salazarev.hw27servicesrecorder.record.RecordListener {
                override fun isRecordered() {
                    if (checkFindFilePermission()) updateAdapter(viewModel.getRecordItems())
                    recordUnbind()
                }

            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundRecordService = false
        }
    }

        private val playConnection: ServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(
                className: ComponentName,
                _service: IBinder
            ) {
                val binder = _service as PlayService.LocalPlayServiceBinder
                playService = binder.getService()
                boundPlayService = true
                playService.setListener(object : PlayListener {
                    override fun isPlay(playStatus: AudioPlayer.PlayState, fileName: String) {
                        when (playStatus){
                           AudioPlayer.PlayState.PLAY ->   adapter.itemPlayStatus(playStatus, fileName)
                            AudioPlayer.PlayState.PAUSE -> adapter.itemPlayStatus(playStatus, fileName)
                            AudioPlayer.PlayState.STOP -> {
                                adapter.itemPlayStatus(playStatus, fileName)
                                playUnbind()}
                        }
                    }
                })
                playService.startMyService()
            }

        override fun onServiceDisconnected(arg0: ComponentName) {
            boundPlayService = false
        }
    }

    private fun recordUnbind() {
        unbindService(recordConnection)
        val intent = Intent(this, RecordService::class.java)
        stopService(intent)
    }

    private fun playUnbind() {
        unbindService(playConnection)
        val intent = Intent(this, PlayService::class.java)
        stopService(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setToolbar()

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        adapter = RecordsAdapter(emptyList(),
            object : RecordListener {
                override fun onclick(fileName: String) {
                     startPlayService("${viewModel.dir}/$fileName")
                }
            })
        setRecyclerView(adapter)
    }

    override fun onResume() {
        super.onResume()
        if (checkFindFilePermission()) updateAdapter(viewModel.getRecordItems())
    }

    private fun updateAdapter(data: List<RecordItem>) {
        adapter.updateData(data)
    }


    private fun setToolbar() {
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_toolbar)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.btn_record -> {
                        if (checkRecordPermission()) startRecordService()
                        true
                    }
                    else -> super.onOptionsItemSelected(it)
                }
            }
        }
    }


    private fun checkRecordPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            )
            == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            == PackageManager.PERMISSION_GRANTED
        ) true
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_RECORD_CODE
            )
            false
        }
    }

    private fun checkFindFilePermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            == PackageManager.PERMISSION_GRANTED
        ) true
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_FIND_FILE_CODE
            )
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_RECORD_CODE) {
            for (permission in grantResults) {
                if (permission == PackageManager.PERMISSION_GRANTED) startRecordService()
            }
        } else if (requestCode == REQUEST_FIND_FILE_CODE) {
            for (permission in grantResults) {
                if (permission == PackageManager.PERMISSION_GRANTED) updateAdapter(viewModel.getRecordItems())
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setRecyclerView(adapter: RecordsAdapter) {
        binding.rvRecords.adapter = adapter
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.rvRecords.layoutManager = layoutManager
        binding.rvRecords.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
    }

    private fun startRecordService() {
        val intent = Intent(this, RecordService::class.java)
        intent.action = RecordService.ACTION_START_SERVICE
        bindService(intent, recordConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startPlayService(dir: String) {
        val intent = Intent(this, PlayService::class.java)
        intent.putExtra(PlayService.DIRECTORY_KEY,dir)
        bindService(intent, playConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        unbindService(playConnection)
        unbindService(recordConnection)
        super.onDestroy()
    }
}