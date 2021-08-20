package com.shencoder.webrtc_srs.http

import com.elvishew.xlog.XLog
import com.shencoder.mvvmkit.http.BaseRetrofitClient
import com.shencoder.webrtc_srs.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 *
 * @author  ShenBen
 * @date    2021/8/19 12:38
 * @email   714081644@qq.com
 */

class RetrofitClient : BaseRetrofitClient() {

    val apiService by lazy {
        getApiService(ApiService::class.java, "http://192.168.2.88:1985")
    }

    override fun generateOkHttpBuilder(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        val interceptor = HttpLoggingInterceptor { message -> XLog.i(message) }
        interceptor.level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        return builder.addInterceptor(interceptor)
    }

    override fun generateRetrofitBuilder(builder: Retrofit.Builder): Retrofit.Builder {
        return builder.apply {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            addConverterFactory(MoshiConverterFactory.create(moshi))
        }

    }
}