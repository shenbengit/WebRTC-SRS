package com.shencoder.webrtc_srs

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.shencoder.mvvmkit.base.view.BaseSupportActivity
import com.shencoder.mvvmkit.base.viewmodel.DefaultViewModel
import com.shencoder.mvvmkit.util.MoshiUtil
import com.shencoder.mvvmkit.util.toastError
import com.shencoder.mvvmkit.util.toastWarning
import com.shencoder.webrtc_srs.constant.Constant
import com.shencoder.webrtc_srs.databinding.ActivityMainBinding
import com.shencoder.webrtc_srs.http.RetrofitClient
import com.shencoder.webrtc_srs.http.bean.SrsRequestBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.webrtc.*


class MainActivity : BaseSupportActivity<DefaultViewModel, ActivityMainBinding>() {
    private val retrofitClient by inject<RetrofitClient>()
    val socketIoClient = SocketIoClient()
    private val eglBaseContext = EglBase.create().eglBaseContext
    private lateinit var peerConnectionFactory: PeerConnectionFactory

    /**
     * 推流地址
     */
    private val pushWebrtcUrl =
        "webrtc://${Constant.SRS_SERVER_HTTPS}/live/android/camera"

    private var pushConnection: PeerConnection? = null
    private var pullConnection: PeerConnection? = null

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun getViewModelId(): Int {
        return 0
    }

    override fun injectViewModel(): Lazy<DefaultViewModel> {
        return viewModel()
    }

    override fun initView() {
        val etRoomId = findViewById<EditText>(R.id.etRoomId)
        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val roomId = etRoomId.text.toString().trim()
            if (roomId.isBlank()) {
                toastWarning("请输入房间号")
                return@setOnClickListener
            }
            socketIoClient.run {
                setRoomId(roomId)
                joinRoom()
            }
            initPushRtc()
        }
    }

    override fun initData(savedInstanceState: Bundle?) {
        val options = PeerConnectionFactory.Options()
        val encoderFactory =
            createCustomVideoEncoderFactory(eglBaseContext,
                enableIntelVp8Encoder = true,
                enableH264HighProfile = true,
                videoEncoderSupportedCallback = { info -> //判断编码器是否支持
                    TextUtils.equals(
                        "OMX.rk.video_encoder.avc",
                        info.name
                    )
                })
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        mBinding.surfaceViewLocal.init(eglBaseContext, null)
        mBinding.surfaceViewRemote.init(eglBaseContext, null)

        socketIoClient.setCallback {
            println("pushRTC:${it}")
            if (it.isNullOrBlank()) {
                return@setCallback
            }
            runOnUiThread { initPullRTC(url = it) }
        }
        socketIoClient.connect()
    }

    private fun initPullRTC(url: String) {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        // 这里不能用PLAN_B 会报错
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        val peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnectionObserver() {
                override fun onAddStream(mediaStream: MediaStream?) {
                    super.onAddStream(mediaStream)
                    mediaStream?.let {
                        if (it.videoTracks.isEmpty().not()) {
                            it.videoTracks[0].addSink(mBinding.surfaceViewRemote)
                        }
                    }
                }
            })?.apply {
            addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
            )
            addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
            )
        }

        peerConnection?.let { connection ->
            connection.createOffer(object : SdpAdapter("createOffer") {
                override fun onCreateSuccess(description: SessionDescription?) {
                    super.onCreateSuccess(description)
                    description?.let {
                        if (it.type == SessionDescription.Type.OFFER) {
                            val offerSdp = it.description
                            connection.setLocalDescription(SdpAdapter("setLocalDescription"), it)
                            //"http://192.168.2.150:1985/rtc/v1/publish/"
                            val srsBean = SrsRequestBean(
                                it.description,
                                url
                            )
                            val toJson = MoshiUtil.toJson(srsBean)
                            println("pull-json:${toJson}")
                            //请求srs
                            lifecycleScope.launch {
                                val result = try {
                                    withContext(Dispatchers.IO) {
                                        retrofitClient.apiService.requestRemoteSrs(srsBean)
                                    }
                                } catch (e: Exception) {
                                    println("pull网络请求出错：${e.printStackTrace()}")
                                    toastError("pull网络请求出错：${e.printStackTrace()}")
                                    null
                                }

                                result?.let { bean ->
                                    if (bean.code == 0) {
                                        XLog.i("pull网络成功，code：${bean.code}")
                                        val remoteSdp = SessionDescription(
                                            SessionDescription.Type.ANSWER,
                                            reorderSdp(offerSdp, bean.sdp)
                                        )
                                        connection.setRemoteDescription(
                                            SdpAdapter("setRemoteDescription"),
                                            remoteSdp
                                        )
                                    } else {
                                        XLog.w("pull网络请求失败，code：${bean.code}")
                                        toastWarning("pull网络请求失败，code：${bean.code}")
                                    }
                                }
                            }
                        }
                    }
                }
            }, MediaConstraints())
        }

        pullConnection = peerConnection
    }

    private fun reorderSdp(offerSdp: String, sdp: String?): String {
        if (sdp.isNullOrBlank()) {
            return ""
        }
        if (offerSdp.isEmpty()) {
            return sdp
        }
        val offerFirstM = offerSdp.substring(offerSdp.indexOf("m="), offerSdp.lastIndexOf("m="))
        val firstM = sdp.substring(sdp.indexOf("m="), sdp.lastIndexOf("m="))
        if (offerFirstM.indexOf("m=video") == firstM.indexOf("m=video")) {
            return sdp
        }
        val start = sdp.substring(0, sdp.indexOf("m="))
        val lastM = sdp.substring(sdp.lastIndexOf("m="), sdp.length)
        val str = start + lastM + firstM
//        XLog.d("reOrderSdp $str")
        return str
    }


    private fun initPushRtc() {
        val createAudioSource = peerConnectionFactory.createAudioSource(createAudioConstraints())
        val audioTrack =
            peerConnectionFactory.createAudioTrack("local_audio_track", createAudioSource)

        val videoCapture = createVideoCapture(this)
        var videoTrack: VideoTrack? = null
        videoCapture?.let { capture ->
            val videoSource = peerConnectionFactory.createVideoSource(capture.isScreencast)
            videoTrack =
                peerConnectionFactory.createVideoTrack("local_video_track", videoSource)
            val textureHelper =
                SurfaceTextureHelper.create("surface_texture_thread", eglBaseContext)
            capture.initialize(textureHelper, this, videoSource.capturerObserver)
            //使用720P的分辨率
            capture.startCapture(1280, 720, 30)
            videoTrack!!.addSink(mBinding.surfaceViewLocal)
        }

        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        // 这里不能用PLAN_B 会报错
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        val peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            PeerConnectionObserver()
        )?.apply {
            if (videoTrack != null) {
                addTransceiver(
                    videoTrack,
                    RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY)
                )
            }
            addTransceiver(
                audioTrack,
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY)
            )
        }

        peerConnection?.let { connection ->
            connection.createOffer(object : SdpAdapter("createOffer") {
                override fun onCreateSuccess(description: SessionDescription?) {
                    super.onCreateSuccess(description)
                    description?.let {
                        if (it.type == SessionDescription.Type.OFFER) {
                            val offerSdp = it.description
                            connection.setLocalDescription(SdpAdapter("setLocalDescription"), it)
                            //"http://192.168.2.150:1985/rtc/v1/publish/"

                            val srsBean = SrsRequestBean(
                                it.description,
                                pushWebrtcUrl
                            )

                            val toJson = MoshiUtil.toJson(srsBean)
                            println("push-json:${toJson}")
                            //请求srs
                            lifecycleScope.launch {
                                val result = try {
                                    withContext(Dispatchers.IO) {
                                        retrofitClient.apiService.pullToSrs(srsBean)
                                    }
                                } catch (e: Exception) {
                                    println("push网络请求出错：${e.printStackTrace()}")
                                    toastError("push网络请求出错：${e.printStackTrace()}")
                                    null
                                }

                                result?.let { bean ->
                                    if (bean.code == 0) {
                                        XLog.i("push网络成功，code：${bean.code}")
                                        val remoteSdp = SessionDescription(
                                            SessionDescription.Type.ANSWER,
                                            reorderSdp(offerSdp, bean.sdp)
                                        )
                                        connection.setRemoteDescription(
                                            SdpAdapter("setRemoteDescription"),
                                            remoteSdp
                                        )
                                        socketIoClient.pullWebRTC(pushWebrtcUrl)
                                    } else {
                                        XLog.w("push网络请求失败，code：${bean.code}")
                                        toastWarning("push网络请求失败，code：${bean.code}")
                                    }
                                }
                            }
                        }
                    }
                }
            }, MediaConstraints())
        }

        pushConnection = peerConnection
    }

    private fun createAudioConstraints(): MediaConstraints {
        val audioConstraints = MediaConstraints()
        //回声消除
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googEchoCancellation",
                "true"
            )
        )
        //自动增益
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        //高音过滤
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        //噪音处理
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googNoiseSuppression",
                "true"
            )
        )
        return audioConstraints
    }

    private fun createVideoCapture(context: Context): CameraVideoCapturer? {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator()
        }

        for (name in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null)
            }
        }
        for (name in enumerator.deviceNames) {
            if (enumerator.isBackFacing(name)) {
                return enumerator.createCapturer(name, null)
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        socketIoClient.close()
        pushConnection?.dispose()
        pullConnection?.dispose()
        peerConnectionFactory.dispose()
    }
}