package com.shencoder.http_callback.bean


import com.squareup.moshi.Json

data class OnUnpublishBean(
    @Json(name = "action")
    val action: String,
    @Json(name = "app")
    val app: String,
    @Json(name = "client_id")
    val clientId: String,
    @Json(name = "ip")
    val ip: String,
    @Json(name = "stream")
    val stream: String,
    @Json(name = "vhost")
    val vhost: String
)