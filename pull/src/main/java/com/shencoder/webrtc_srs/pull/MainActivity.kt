package com.shencoder.webrtc_srs.pull

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.shencoder.mvvmkit.base.view.BaseSupportActivity
import com.shencoder.mvvmkit.base.viewmodel.DefaultViewModel
import com.shencoder.mvvmkit.util.MoshiUtil
import com.shencoder.mvvmkit.util.toastError
import com.shencoder.mvvmkit.util.toastWarning
import com.shencoder.webrtc_srs.pull.databinding.ActivityMainBinding
import com.shencoder.webrtc_srs.pull.http.RetrofitClient
import com.shencoder.webrtc_srs.pull.http.bean.SrsRequestBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.webrtc.*

class MainActivity : BaseSupportActivity<DefaultViewModel, ActivityMainBinding>() {
    private val retrofitClient by inject<RetrofitClient>()

    private val eglBaseContext = EglBase.create().eglBaseContext
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var pullConnection: PeerConnection? = null
    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun injectViewModel(): Lazy<DefaultViewModel> {
        return viewModel()
    }

    override fun getViewModelId(): Int {
        return 0
    }

    override fun initView() {
        mBinding.svr.init(eglBaseContext, null)
        mBinding.btnPull.setOnClickListener {
            val url = mBinding.etUrl.text.toString().trim()
            if (url.isBlank()) {
                toastWarning("请输入拉流地址")
                return@setOnClickListener
            }
            initPullRTC(url)
        }

    }

    override fun initData(savedInstanceState: Bundle?) {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(applicationContext).createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        val encoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
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
                            it.videoTracks[0].addSink(mBinding.svr)
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

    override fun onDestroy() {
        super.onDestroy()
        pullConnection?.dispose()
        peerConnectionFactory.dispose()
    }
}