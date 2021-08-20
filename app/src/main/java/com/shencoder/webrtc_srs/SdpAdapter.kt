package com.shencoder.webrtc_srs

import com.elvishew.xlog.XLog
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * @author ShenBen
 * @date 2020/4/1 15:00
 * @email 714081644@qq.com
 */
open class SdpAdapter constructor(private val tag: String) : SdpObserver {

    override fun onSetFailure(str: String?) {
        XLog.e("SdpObserver->${tag}->onSetFailure:$str")
    }

    override fun onSetSuccess() {
        XLog.i("SdpObserver->${tag}->onSetSuccess")
    }

    override fun onCreateSuccess(description: SessionDescription?) {
        XLog.i("SdpObserver->${tag}->onCreateSuccess")
    }

    override fun onCreateFailure(s: String?) {
        XLog.e("SdpObserver->${tag}->onCreateFailure")
    }
}