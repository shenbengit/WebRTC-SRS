package com.shencoder.http_callback.bean


import com.squareup.moshi.Json

data class OnConnectBean(
    @Json(name = "action")
    val action: String,
    @Json(name = "app")
    val app: String,
    @Json(name = "client_id")
    val clientId: String,
    @Json(name = "ip")
    val ip: String,
    @Json(name = "pageUrl")
    val pageUrl: String,
    @Json(name = "tcUrl")
    val tcUrl: String,
    @Json(name = "vhost")
    val vhost: String
)