package com.shencoder.http_callback.bean

import com.squareup.moshi.Json

/**
 *
 * @author  ShenBen
 * @date    2021/12/6 17:10
 * @email   714081644@qq.com
 */
data class RespBean(
    @Json(name = "code")
    val code: Int,
    @Json(name = "data")
    val data: String?
)