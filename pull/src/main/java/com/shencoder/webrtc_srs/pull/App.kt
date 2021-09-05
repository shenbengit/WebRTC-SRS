package com.shencoder.webrtc_srs.pull

import android.app.Application
import com.shencoder.mvvmkit.ext.globalInit
import com.shencoder.webrtc_srs.pull.di.appModule
import org.koin.android.java.KoinAndroidApplication
import org.koin.core.logger.Level

/**
 *
 * @author  ShenBen
 * @date    2021/8/19 12:41
 * @email   714081644@qq.com
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        val koinApplication =
            KoinAndroidApplication
                .create(
                    this,
                    if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR
                )
                .modules(appModule)
        globalInit(BuildConfig.DEBUG, "WebRTC-SRS-PUSH", koinApplication)
    }
}