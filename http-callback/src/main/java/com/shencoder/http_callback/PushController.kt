package com.shencoder.http_callback

import com.shencoder.http_callback.bean.*
import com.shencoder.http_callback.util.MoshiUtil
import com.yanzhenjie.andserver.annotation.*

/**
 * [https://github.com/ossrs/srs/wiki/v4_CN_HTTPCallback]
 *
 * @author  ShenBen
 * @date    2021/12/6 17:06
 * @email   714081644@qq.com
 */
@RestController
@CrossOrigin(methods = [RequestMethod.POST, RequestMethod.GET])
@RequestMapping(
    path = ["/api/srs/push"],
    method = [RequestMethod.POST, RequestMethod.GET]
)
class PushController {
    private companion object {
        /**
         * 0标识成功，必须返回0，否则会出问题
         * 其他错误码会断开客户端连接。
         */
        private const val CODE_SUCCESS = 0
    }

    /**
     * 当客户端连接到指定的vhost和app时，仅在推流时调用
     */
    @PostMapping(path = ["/onConnect"])
    fun onConnect(@RequestBody bean: OnConnectBean): RespBean {
        println("on_connect:${MoshiUtil.toJson(bean)}")
        return RespBean(CODE_SUCCESS, "success")
    }

    /**
     * 当客户端关闭连接，或者SRS主动关闭连接时，仅在推流时调用
     */
    @PostMapping(path = ["/onClose"])
    fun onClose(@RequestBody bean: OnCloseBean): RespBean {
        println("on_close:${MoshiUtil.toJson(bean)}")
        return RespBean(CODE_SUCCESS, "success")
    }

    /**
     * 当客户端发布流时
     */
    @PostMapping(path = ["/onPublish"])
    fun onPublish(@RequestBody bean: OnPublishBean): RespBean {
        println("on_publish:${MoshiUtil.toJson(bean)}")
        return RespBean(CODE_SUCCESS, "success")
    }

    /**
     * 当客户端停止发布流时
     */
    @PostMapping(path = ["/onUnpublish"])
    fun onUnpublish(@RequestBody bean: OnUnpublishBean): RespBean {
        println("on_unpublish:${MoshiUtil.toJson(bean)}")
        return RespBean(CODE_SUCCESS, "success")
    }

    /**
     * 当客户端开始播放流时
     */
    @PostMapping(path = ["/onPlay"])
    fun onPlay(@RequestBody bean: OnPlayBean): RespBean {
        println("on_play:${MoshiUtil.toJson(bean)}")
        return RespBean(CODE_SUCCESS, "success")
    }

    /**
     * 当客户端停止播放时。备注：停止播放可能不会关闭连接，还能再继续播放。
     */
    @PostMapping(path = ["/onStop"])
    fun onStop(@RequestBody bean: OnStopBean): RespBean {
        println("on_stop:${MoshiUtil.toJson(bean)}")
        return RespBean(CODE_SUCCESS, "success")
    }

    /**
     * 当DVR录制关闭一个flv文件时，录制完成之后才调用。
     */
    @PostMapping(path = ["/onDvr"])
    fun onDvr(@RequestBody bean: OnDvrBean): RespBean {
        println("on_dvr:${MoshiUtil.toJson(bean)}")
        return RespBean(CODE_SUCCESS, "success")
    }


}