package com.shencoder.webrtc_srs.pull.di

import com.shencoder.webrtc_srs.pull.http.RetrofitClient
import org.koin.dsl.module

/**
 *
 * @author  ShenBen
 * @date    2021/6/10 11:19
 * @email   714081644@qq.com
 */

private val singleModule = module {
    single { RetrofitClient() }
}


val appModule = mutableListOf(singleModule).apply {

}