package com.shencoder.webrtc_srs.push.http

import com.shencoder.webrtc_srs.push.http.bean.SrsRequestBean
import com.shencoder.webrtc_srs.push.http.bean.SrsResponseBean
import retrofit2.http.Body
import retrofit2.http.POST

/**
 *
 * @author  ShenBen
 * @date    2021/8/19 12:38
 * @email   714081644@qq.com
 */
interface ApiService {

    @POST("/rtc/v1/play/")
    suspend fun play(@Body body: SrsRequestBean): SrsResponseBean

    @POST("/rtc/v1/publish/")
    suspend fun publish(@Body body: SrsRequestBean): SrsResponseBean
}