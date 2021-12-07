package com.shencoder.http_callback

import com.shencoder.http_callback.util.MoshiUtil
import com.yanzhenjie.andserver.annotation.Converter
import com.yanzhenjie.andserver.framework.MessageConverter
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.IOUtils
import com.yanzhenjie.andserver.util.MediaType
import java.io.InputStream
import java.lang.reflect.Type

/**
 * @author ShenBen
 * @date 2020/3/13 9:27
 * @email 714081644@qq.com
 */
@Converter
class BodyConverter : MessageConverter {

    override fun convert(output: Any?, mediaType: MediaType?): ResponseBody {
        return StringBody(
            MoshiUtil.toJson(output), mediaType ?: MediaType.APPLICATION_JSON_UTF8
        )
    }

    override fun <T : Any?> convert(stream: InputStream, mediaType: MediaType?, type: Type?): T? {
        type?.let {
            val charset = mediaType?.charset
                ?: return MoshiUtil.fromJson<T>(IOUtils.toString(stream), it)
            return MoshiUtil.fromJson<T>(IOUtils.toString(stream, charset), it)
        }
        return null
    }
}