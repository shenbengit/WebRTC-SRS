package com.shencoder.webrtc_srs


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SrsResponseBean(
    @Json(name = "code")
    val code: Int,
    @Json(name = "sdp")
    val sdp: String?,
    @Json(name = "server")
    val server: String?,
    @Json(name = "sessionid")
    val sessionid: String?
)