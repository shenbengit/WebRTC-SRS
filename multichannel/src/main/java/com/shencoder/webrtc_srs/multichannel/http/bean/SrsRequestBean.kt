package com.shencoder.webrtc_srs.multichannel.http.bean


import com.squareup.moshi.Json

data class SrsRequestBean(
    @Json(name = "sdp")
    val sdp: String?,
    @Json(name = "streamurl")
    val streamUrl: String?
)