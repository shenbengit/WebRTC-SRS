package com.shencoder.webrtc_srs.multichannel

import com.elvishew.xlog.XLog
import org.webrtc.*

/**
 * @author ShenBen
 * @date 2020/4/1 13:56
 * @email 714081644@qq.com
 */
open class PeerConnectionObserver : PeerConnection.Observer {

    companion object {
        private const val TAG = "PeerConnectionObserver->"
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        XLog.i("${TAG}onIceCandidate")
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
        XLog.i("${TAG}onDataChannel")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        XLog.i("${TAG}onIceConnectionReceivingChange:$p0")
    }

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
        XLog.i("${TAG}onIceConnectionChange:${iceConnectionState}")
    }

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
        XLog.i("${TAG}onConnectionChange:${newState}")
    }

    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState?) {
        XLog.i("${TAG}onIceGatheringChange:${iceGatheringState}")
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        XLog.i("${TAG}onAddStream")
    }

    override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {
        XLog.i("${TAG}onSignalingChange:${signalingState}")
    }

    override fun onIceCandidatesRemoved(array: Array<out IceCandidate>?) {
        XLog.i("${TAG}onIceCandidatesRemoved")
    }

    override fun onRemoveStream(mediaStream: MediaStream?) {
        XLog.i("${TAG}onRemoveStream")
    }

    override fun onRenegotiationNeeded() {
        XLog.i("${TAG}onRenegotiationNeeded")
    }

    override fun onAddTrack(rtpReceiver: RtpReceiver?, array: Array<out MediaStream>?) {
        XLog.i("${TAG}onDataChannel")
    }
}