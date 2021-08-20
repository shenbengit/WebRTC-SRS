package org.webrtc;

/**
 * @author ShenBen
 * @date 2021/8/19 15:36
 * @email 714081644@qq.com
 */
public class H264VideoEncoderFactory extends DefaultVideoEncoderFactory {

    public H264VideoEncoderFactory(EglBase.Context eglContext, boolean enableIntelVp8Encoder, boolean enableH264HighProfile) {
        super(eglContext, enableIntelVp8Encoder, enableH264HighProfile);
    }

    public H264VideoEncoderFactory(VideoEncoderFactory hardwareVideoEncoderFactory) {
        super(hardwareVideoEncoderFactory);
    }
}
