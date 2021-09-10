package com.shencoder.webrtc_srs.http

import com.shencoder.webrtc_srs.http.bean.SrsRequestBean
import com.shencoder.webrtc_srs.http.bean.SrsResponseBean
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
    suspend fun requestRemoteSrs(@Body body: SrsRequestBean): SrsResponseBean

    @POST("/rtc/v1/publish/")
    suspend fun pullToSrs(@Body body: SrsRequestBean): SrsResponseBean
}