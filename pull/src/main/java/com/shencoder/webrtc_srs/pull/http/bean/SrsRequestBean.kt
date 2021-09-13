package com.shencoder.webrtc_srs.pull.http.bean


import com.squareup.moshi.Json
import org.webrtc.PeerConnection

data class SrsRequestBean(
    /**
     * [PeerConnection.createOffer]返回的sdp
     */
    @Json(name = "sdp")
    val sdp: String?,
    /**
     * 拉取的WebRTC流地址
     */
    @Json(name = "streamurl")
    val streamUrl: String?
)