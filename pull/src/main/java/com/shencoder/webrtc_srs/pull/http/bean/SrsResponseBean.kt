package com.shencoder.webrtc_srs.pull.http.bean


import com.squareup.moshi.Json
import org.webrtc.PeerConnection

data class SrsResponseBean(
    /**
     * 0：成功
     */
    @Json(name = "code")
    val code: Int,
    /**
     * 用于设置[PeerConnection.setRemoteDescription]
     */
    @Json(name = "sdp") val sdp: String?,
    @Json(name = "server")
    val server: String?,
    @Json(name = "sessionid")
    val sessionId: String?
)