package com.shencoder.http_callback.util

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.Type

/**
 *
 * @author  ShenBen
 * @date    2021/12/6 17:07
 * @email   714081644@qq.com
 */
object MoshiUtil {

    private var sMoshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @JvmStatic
    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        val adapter = sMoshi.adapter(clazz)
        return adapter.fromJson(json)
    }

    @JvmStatic
    fun <T> fromJson(json: String, type: Type): T? {
        val adapter = sMoshi.adapter<T>(type)
        return adapter.fromJson(json)
    }

    @JvmStatic
    fun toJson(bean: Any?): String {
        if (bean == null) {
            return "{}"
        }
        val adapter = sMoshi.adapter(bean.javaClass)
        return adapter.toJson(bean)
    }
}