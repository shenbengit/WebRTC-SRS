package com.shencoder.http_callback

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server

class MainActivity : AppCompatActivity() {
    private companion object {
        private const val TAG = "MainActivity"
    }

    /**
     * Get、Post请求
     */
    private lateinit var andServer: Server

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initAndServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        andServer.shutdown()
    }

    private fun initAndServer() {
        andServer = AndServer.webServer(this)
            .port(9099)
            .listener(object : Server.ServerListener {
                override fun onException(e: java.lang.Exception?) {
                    Log.e(TAG, "andServer-onException:${e?.message}")
                }

                override fun onStarted() {
                    Log.i(TAG, "andServer-onStarted")
                }

                override fun onStopped() {
                    Log.w(TAG, "andServer-onStopped")
                }
            })
            .build()
        andServer.startup()
    }
}