package com.shencoder.webrtc_srs.multichannel.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.elvishew.xlog.XLog
import com.shencoder.mvvmkit.util.MoshiUtil
import com.shencoder.webrtc_srs.multichannel.PeerConnectionObserver
import com.shencoder.webrtc_srs.multichannel.R
import com.shencoder.webrtc_srs.multichannel.SdpAdapter
import com.shencoder.webrtc_srs.multichannel.http.RetrofitClient
import com.shencoder.webrtc_srs.multichannel.http.bean.SrsRequestBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * 使用此自定义view之前你必须要调用一次：
 *
 * PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(applicationContext).createInitializationOptions())
 *
 * @author  ShenBen
 * @date    2021/9/7 14:51
 * @email   714081644@qq.com
 */
class MultichannelCallLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), KoinComponent {

    private val cameraSwitchHandler = object : CameraVideoCapturer.CameraSwitchHandler {
        override fun onCameraSwitchDone(isFrontCamera: Boolean) {
            post {

            }
        }

        override fun onCameraSwitchError(errorDescription: String?) {
            post {

            }
        }
    }

    private val nineGridLayout: NineGridLayout
    private val clMeetingAction: ConstraintLayout
    private val tvSpeakerMute: TextView
    private val tvSwitchCamera: TextView
    private val tvSpeaker: TextView
    private val tvHangup: TextView

    private var isSpeakerMute = false

    private val retrofitClient by inject<RetrofitClient>()
    private val peerConnectionFactory: PeerConnectionFactory
    private val audioDeviceModule: JavaAudioDeviceModule
    private val eglBaseContext = EglBase.create().eglBaseContext
    private var mPublishCallBean: PublishCallBean? = null
    private val mPlayConnectionMap: MutableMap<CallView, PeerConnection> =
        LinkedHashMap(NineGridLayout.MAX_CHILD_COUNT)

    private var isRelease = false

    private companion object {
        private val URLS = listOf(
            "webrtc://192.168.2.87/live/livestream",
            "webrtc://192.168.2.87:1990/web/livestream1",
            "webrtc://192.168.2.87/live/livestream",
            "webrtc://192.168.2.87:1990/web/livestream1",
            "webrtc://192.168.2.87/live/livestream",
            "webrtc://192.168.2.87/live/livestream",
            "webrtc://192.168.2.87/live/livestream",
            "webrtc://192.168.2.87/live/livestream",
        )
    }

    init {
        val options = PeerConnectionFactory.Options()
        val encoderFactory =
            createCustomVideoEncoderFactory(eglBaseContext,
                enableIntelVp8Encoder = true,
                enableH264HighProfile = true,
                videoEncoderSupportedCallback = { info -> //判断编码器是否支持
//                    TextUtils.equals(
//                        "OMX.rk.video_encoder.avc",
//                        info.name
//                    )
                    true
                })
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        audioDeviceModule =
            JavaAudioDeviceModule.builder(context).createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()

        LayoutInflater.from(context).inflate(R.layout.layout_multichannel_call, this)
        nineGridLayout = findViewById(R.id.nineGridLayout)
        clMeetingAction = findViewById(R.id.clMeetingAction)
        tvSpeakerMute = findViewById(R.id.tvSpeakerMute)
        tvSwitchCamera = findViewById(R.id.tvSwitchCamera)
        tvSpeaker = findViewById(R.id.tvSpeaker)
        tvHangup = findViewById(R.id.tvHangup)
        tvSpeakerMute.setOnClickListener {
            isSpeakerMute = isSpeakerMute.not()
            audioDeviceModule.setSpeakerMute(isSpeakerMute)
            tvSpeakerMute.isSelected = isSpeakerMute
        }
        tvSwitchCamera.setOnClickListener {
            mPublishCallBean?.cameraVideoCapturer?.switchCamera(cameraSwitchHandler)
        }

        tvSpeaker.setOnClickListener {


        }
        tvHangup.setOnClickListener {
            release()
        }
    }

    @Synchronized
    fun release() {
        if (isRelease) {
            return
        }
        mPlayConnectionMap.forEach {
            it.key.release()
            it.value.dispose()
        }
        mPlayConnectionMap.clear()
        mPublishCallBean?.release()
        mPublishCallBean = null
        audioDeviceModule.release()
        peerConnectionFactory.dispose()
        isRelease = true
    }

    /**
     * 添加通话显示View
     * 推流只能有一个，拉流可以多个
     * @param url 推流或拉流地址
     * @param isPublish true：推流，false：拉流
     */
    @JvmOverloads
    fun addCallView(url: String, isPublish: Boolean = false): Boolean {
        if (childCount >= NineGridLayout.MAX_CHILD_COUNT) {
            return false
        }
        if (isPublish && mPublishCallBean != null) {
            return false
        }
        val callView = CallView(context)
        callView.init(eglBaseContext, null)
        if (isPublish) {
            addView(callView, 0)
            publish(url, callView)
        } else {
            addView(callView)
            play(url, callView)
        }
        return true
    }

    private fun publish(url: String, callView: CallView) {
        val createAudioSource = peerConnectionFactory.createAudioSource(createAudioConstraints())
        val audioTrack =
            peerConnectionFactory.createAudioTrack("local_audio_track", createAudioSource)

        val videoCapture = createVideoCapture(context)
        var videoTrack: VideoTrack? = null
        var surfaceTextureHelper: SurfaceTextureHelper? = null
        videoCapture?.let { capture ->
            val videoSource = peerConnectionFactory.createVideoSource(capture.isScreencast)

            surfaceTextureHelper =
                SurfaceTextureHelper.create("surface_texture_thread", eglBaseContext)
            capture.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            capture.startCapture(640, 480, 25)
            videoTrack =
                peerConnectionFactory.createVideoTrack("local_video_track", videoSource).apply {
                    addSink(callView)
                }
        }

        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        // 这里不能用PLAN_B 会报错
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnectionFactory.createPeerConnection(
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
            createOffer(object : SdpAdapter("createOffer") {
                override fun onCreateSuccess(description: SessionDescription?) {
                    super.onCreateSuccess(description)
                    description?.let {
                        if (it.type == SessionDescription.Type.OFFER) {
                            val offerSdp = it.description
                            setLocalDescription(SdpAdapter("setLocalDescription"), it)

                            val srsBean = SrsRequestBean(
                                it.description,
                                url
                            )

                            val toJson = MoshiUtil.toJson(srsBean)
                            println("push-json:${toJson}")
                            //请求srs
                            GlobalScope.launch {
                                val result = try {
                                    withContext(Dispatchers.IO) {
                                        retrofitClient.apiService.publish(srsBean)
                                    }
                                } catch (e: Exception) {
                                    println("push网络请求出错：${e.printStackTrace()}")
                                    null
                                }

                                result?.let { bean ->
                                    if (bean.code == 0) {
                                        XLog.i("push网络成功，code：${bean.code}")
                                        val remoteSdp = SessionDescription(
                                            SessionDescription.Type.ANSWER,
                                            convertAnswerSdp(offerSdp, bean.sdp)
                                        )
                                        setRemoteDescription(
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

            mPublishCallBean =
                PublishCallBean(callView, this, videoTrack, videoCapture, surfaceTextureHelper)
        }
    }

    private fun play(url: String, callView: CallView) {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnectionObserver() {
                override fun onAddStream(mediaStream: MediaStream?) {
                    super.onAddStream(mediaStream)
                    mediaStream?.let {
                        if (it.videoTracks.isEmpty().not()) {
                            it.videoTracks[0].addSink(callView)
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

            createOffer(object : SdpAdapter("createOffer") {
                override fun onCreateSuccess(description: SessionDescription?) {
                    super.onCreateSuccess(description)
                    description?.let {
                        if (it.type == SessionDescription.Type.OFFER) {
                            val offerSdp = it.description
                            setLocalDescription(SdpAdapter("setLocalDescription"), it)
                            val srsBean = SrsRequestBean(it.description, url)
                            val toJson = MoshiUtil.toJson(srsBean)
                            println("pull-json:${toJson}")
                            //请求srs
                            GlobalScope.launch {
                                val result = try {
                                    withContext(Dispatchers.IO) {
                                        retrofitClient.apiService.play(srsBean)
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
                                            convertAnswerSdp(offerSdp, bean.sdp)
                                        )
                                        setRemoteDescription(
                                            SdpAdapter("setRemoteDescription"),
                                            remoteSdp
                                        )
                                    } else {
                                        XLog.w("pull网络请求失败，code：${bean.code}")
                                    }
                                }
                            }
                        }
                    }
                }
            }, MediaConstraints())
            mPlayConnectionMap[callView] = this
        }
    }


    /**
     * 转换AnswerSdp
     * @param offerSdp offerSdp：创建offer时生成的sdp
     * @param answerSdp answerSdp：网络请求srs服务器返回的sdp
     * @return 转换后的AnswerSdp
     */
    private fun convertAnswerSdp(offerSdp: String, answerSdp: String?): String {
        if (answerSdp.isNullOrBlank()) {
            return ""
        }
        val indexOfOfferVideo = offerSdp.indexOf("m=video")
        val indexOfOfferAudio = offerSdp.indexOf("m=audio")
        if (indexOfOfferVideo == -1 || indexOfOfferAudio == -1) {
            return answerSdp
        }
        val indexOfAnswerVideo = answerSdp.indexOf("m=video")
        val indexOfAnswerAudio = answerSdp.indexOf("m=audio")
        if (indexOfAnswerVideo == -1 || indexOfAnswerAudio == -1) {
            return answerSdp
        }

        val isFirstOfferVideo = indexOfOfferVideo < indexOfOfferAudio
        val isFirstAnswerVideo = indexOfAnswerVideo < indexOfAnswerAudio
        return if (isFirstOfferVideo == isFirstAnswerVideo) {
            //顺序一致
            answerSdp
        } else {
            //需要调换顺序
            buildString {
                append(answerSdp.substring(0, indexOfAnswerVideo.coerceAtMost(indexOfAnswerAudio)))
                append(
                    answerSdp.substring(
                        indexOfAnswerVideo.coerceAtLeast(indexOfOfferVideo),
                        answerSdp.length
                    )
                )
                append(
                    answerSdp.substring(
                        indexOfAnswerVideo.coerceAtMost(indexOfAnswerAudio),
                        indexOfAnswerVideo.coerceAtLeast(indexOfOfferVideo)
                    )
                )
            }
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
}