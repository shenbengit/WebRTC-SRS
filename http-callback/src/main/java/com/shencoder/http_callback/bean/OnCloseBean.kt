package com.shencoder.http_callback.bean


import com.squareup.moshi.Json

data class OnCloseBean(
    @Json(name = "action")
    val action: String,
    @Json(name = "app")
    val app: String,
    @Json(name = "client_id")
    val clientId: String,
    @Json(name = "ip")
    val ip: String,
    @Json(name = "recv_bytes")
    val recvBytes: Int,
    @Json(name = "send_bytes")
    val sendBytes: Int,
    @Json(name = "vhost")
    val vhost: String
)