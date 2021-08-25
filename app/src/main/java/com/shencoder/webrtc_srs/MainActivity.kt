package com.shencoder.webrtc_srs

import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.shencoder.mvvmkit.base.view.BaseSupportActivity
import com.shencoder.mvvmkit.base.viewmodel.DefaultViewModel
import com.shencoder.mvvmkit.util.MoshiUtil
import com.shencoder.webrtc_srs.databinding.ActivityMainBinding
import com.shencoder.webrtc_srs.http.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.webrtc.*
import org.webrtc.MediaConstraints


class MainActivity : BaseSupportActivity<DefaultViewModel, ActivityMainBinding>() {
    private val retrofitClient by inject<RetrofitClient>()
    val socketIoClient = SocketIoClient()
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
        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            socketIoClient.joinRoom()
            initPullRtc()
        }
    }

    override fun initData(savedInstanceState: Bundle?) {
        socketIoClient.setCallback {
            println("pushRTC:${it}")
            if (it.isNullOrBlank()) {
                return@setCallback
            }
            runOnUiThread { initPushRTC(url = it) }
        }
        socketIoClient.connect()
//        Handler(Looper.getMainLooper()).postDelayed({
//            initPushRTC()
//        }, 5 * 1000L)
    }

    private fun initPushRTC(url: String) {
        val eglBaseContext = EglBase.create().eglBaseContext;
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(applicationContext).createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        val encoderFactory =
            H264VideoEncoderFactory(H264HardwareVideoEncoderFactory(eglBaseContext, true, true))
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        val peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        mBinding.surfaceViewRemote.init(eglBaseContext, null)
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
                            offerSdp = it.description
                            connection.setLocalDescription(SdpAdapter("setLocalDescription"), it)
                            //"http://192.168.2.150:1985/rtc/v1/publish/"
                            val srsBean = SrsRequestBean(
                                it.description,
                                url
                            )
                            val toJson = MoshiUtil.toJson(srsBean)
                            println("push-json:${toJson}")
                            //请求srs
                            lifecycleScope.launch {
                                val result = try {
                                    withContext(Dispatchers.IO) {
                                        retrofitClient.apiService.requestRemoteSrs(srsBean)
                                    }
                                } catch (e: Exception) {
                                    println("push网络请求出错：${e.printStackTrace()}")
                                    null
                                }

                                result?.let { bean ->
                                    if (bean.code == 0) {
                                        XLog.i("psuh网络成功，code：${bean.code}")
                                        val remoteSdp = SessionDescription(
                                            SessionDescription.Type.ANSWER,
                                            reorderSdp(bean.sdp)

                                        )
                                        connection.setRemoteDescription(
                                            SdpAdapter("setRemoteDescription"),
                                            remoteSdp
                                        )
                                    } else {
                                        XLog.w("push网络请求失败，code：${bean.code}")
                                    }
                                }
                            }
                        }
                    }
                }
            }, MediaConstraints())
        }
    }

    private var offerSdp: String = ""
    private fun reorderSdp(sdp: String?): String {
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


    private fun initPullRtc() {
        val eglBaseContext = EglBase.create().eglBaseContext;
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(applicationContext).createInitializationOptions()
        )
        mBinding.surfaceViewLocal.init(eglBaseContext, null)

        val options = PeerConnectionFactory.Options()
        val encoderFactory =
            H264VideoEncoderFactory(H264HardwareVideoEncoderFactory(eglBaseContext, true, true))
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        val peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

//        val mediaStream = peerConnectionFactory.createLocalMediaStream("local_media_stream")
        val createAudioSource = peerConnectionFactory.createAudioSource(createAudioConstraints())
        val audioTrack =
            peerConnectionFactory.createAudioTrack("local_audio_track", createAudioSource)
//        mediaStream.addTrack(audioTrack)

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
//            mediaStream.addTrack(videoTrack)
        }


        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        // 这里不能用PLAN_B 会报错
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        val peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnectionObserver() {
                override fun onAddStream(mediaStream: MediaStream?) {
                    super.onAddStream(mediaStream)

                }
            })?.apply {
//            addTransceiver(
//                MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
//                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY)
//            )
//            addTransceiver(
//                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
//                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY)
//            )
            if (videoTrack != null) {
                addTransceiver(videoTrack)
            }
            addTransceiver(audioTrack)

        }

        peerConnection?.let { connection ->
            connection.createOffer(object : SdpAdapter("createOffer") {
                override fun onCreateSuccess(description: SessionDescription?) {
                    super.onCreateSuccess(description)
                    description?.let {
                        if (it.type == SessionDescription.Type.OFFER) {
                            offerSdp = it.description
                            connection.setLocalDescription(SdpAdapter("setLocalDescription"), it)
                            //"http://192.168.2.150:1985/rtc/v1/publish/"

                            val webrtcUrl = "webrtc://192.168.2.88/live/android/camera2"
                            val srsBean = SrsRequestBean(
                                it.description,
                                webrtcUrl
                            )

                            val toJson = MoshiUtil.toJson(srsBean)
                            println("pull-json:${toJson}")
                            //请求srs
                            lifecycleScope.launch {
                                val result = try {
                                    withContext(Dispatchers.IO) {
                                        retrofitClient.apiService.pullToSrs(srsBean)
                                    }
                                } catch (e: Exception) {
                                    println("pull网络请求出错：${e.printStackTrace()}")
                                    null
                                }

                                result?.let { bean ->
                                    if (bean.code == 0) {
                                        XLog.i("pull网络成功，code：${bean.code}")
                                        val remoteSdp = SessionDescription(
                                            SessionDescription.Type.ANSWER,
                                            reorderSdp(bean.sdp)
                                        )
                                        connection.setRemoteDescription(
                                            SdpAdapter("setRemoteDescription"),
                                            remoteSdp
                                        )
                                        socketIoClient.pullWebRTC(webrtcUrl)
                                    } else {
                                        XLog.w("pull网络请求失败，code：${bean.code}")
                                    }
                                }
                            }
                        }
                    }
                }
            }, MediaConstraints())
        }
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
    }
}