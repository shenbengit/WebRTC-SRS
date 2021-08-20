package org.webrtc;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import org.webrtc.BaseBitrateAdjuster;
import org.webrtc.BitrateAdjuster;
import org.webrtc.DynamicBitrateAdjuster;
import org.webrtc.EglBase14;
import org.webrtc.FramerateBitrateAdjuster;
import org.webrtc.H264Utils;
import org.webrtc.HardwareVideoEncoder;
import org.webrtc.Logging;
import org.webrtc.MediaCodecUtils;
import org.webrtc.MediaCodecWrapperFactoryImpl;
import org.webrtc.Predicate;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoCodecMimeType;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ShenBen
 * @date 2021/8/19 15:32
 * @email 714081644@qq.com
 */
public class H264HardwareVideoEncoderFactory implements VideoEncoderFactory {
    private static final String TAG = "HardwareVideoEncoderFactory";
    private static final int QCOM_VP8_KEY_FRAME_INTERVAL_ANDROID_L_MS = 15000;
    private static final int QCOM_VP8_KEY_FRAME_INTERVAL_ANDROID_M_MS = 20000;
    private static final int QCOM_VP8_KEY_FRAME_INTERVAL_ANDROID_N_MS = 15000;
    private static final List<String> H264_HW_EXCEPTION_MODELS = Arrays.asList("SAMSUNG-SGH-I337", "Nexus 7", "Nexus 4");
    @Nullable
    private final EglBase14.Context sharedContext;
    private final boolean enableIntelVp8Encoder;
    private final boolean enableH264HighProfile;
    @Nullable
    private final Predicate<MediaCodecInfo> codecAllowedPredicate;

    public H264HardwareVideoEncoderFactory(org.webrtc.EglBase.Context sharedContext, boolean enableIntelVp8Encoder, boolean enableH264HighProfile) {
        this(sharedContext, enableIntelVp8Encoder, enableH264HighProfile, (Predicate) null);
    }

    public H264HardwareVideoEncoderFactory(org.webrtc.EglBase.Context sharedContext, boolean enableIntelVp8Encoder, boolean enableH264HighProfile, @Nullable Predicate<MediaCodecInfo> codecAllowedPredicate) {
        if (sharedContext instanceof EglBase14.Context) {
            this.sharedContext = (EglBase14.Context) sharedContext;
        } else {
            Logging.w("HardwareVideoEncoderFactory", "No shared EglBase.Context.  Encoders will not use texture mode.");
            this.sharedContext = null;
        }

        this.enableIntelVp8Encoder = enableIntelVp8Encoder;
        this.enableH264HighProfile = enableH264HighProfile;
        this.codecAllowedPredicate = codecAllowedPredicate;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public H264HardwareVideoEncoderFactory(boolean enableIntelVp8Encoder, boolean enableH264HighProfile) {
        this((org.webrtc.EglBase.Context) null, enableIntelVp8Encoder, enableH264HighProfile);
    }

    @Nullable
    public VideoEncoder createEncoder(VideoCodecInfo input) {
        if (Build.VERSION.SDK_INT < 19) {
            return null;
        } else {
            VideoCodecMimeType type = VideoCodecMimeType.valueOf(input.name);
            MediaCodecInfo info = this.findCodecForType(type);
            if (info == null) {
                return null;
            } else {
                String codecName = info.getName();
                String mime = type.mimeType();
                Integer surfaceColorFormat = MediaCodecUtils.selectColorFormat(MediaCodecUtils.TEXTURE_COLOR_FORMATS, info.getCapabilitiesForType(mime));
                Integer yuvColorFormat = MediaCodecUtils.selectColorFormat(MediaCodecUtils.ENCODER_COLOR_FORMATS, info.getCapabilitiesForType(mime));
                if (type == VideoCodecMimeType.H264) {
                    boolean isHighProfile = H264Utils.isSameH264Profile(input.params, MediaCodecUtils.getCodecProperties(type, true));
                    boolean isBaselineProfile = H264Utils.isSameH264Profile(input.params, MediaCodecUtils.getCodecProperties(type, false));
                    if (!isHighProfile && !isBaselineProfile) {
                        return null;
                    }

                    if (isHighProfile && !this.isH264HighProfileSupported(info)) {
                        return null;
                    }
                }

                return new HardwareVideoEncoder(new MediaCodecWrapperFactoryImpl(), codecName, type, surfaceColorFormat, yuvColorFormat, input.params, this.getKeyFrameIntervalSec(type), this.getForcedKeyFrameIntervalMs(type, codecName), this.createBitrateAdjuster(type, codecName), this.sharedContext);
            }
        }
    }

    public VideoCodecInfo[] getSupportedCodecs() {
        if (Build.VERSION.SDK_INT < 19) {
            return new VideoCodecInfo[0];
        } else {
            List<VideoCodecInfo> supportedCodecInfos = new ArrayList<>();
            VideoCodecMimeType[] var2 = new VideoCodecMimeType[]{VideoCodecMimeType.VP8, VideoCodecMimeType.VP9, VideoCodecMimeType.H264};

            for (VideoCodecMimeType type : var2) {
                MediaCodecInfo codec = this.findCodecForType(type);
                if (codec != null) {
                    String name = type.name();
                    if (type == VideoCodecMimeType.H264 && this.isH264HighProfileSupported(codec)) {
                        supportedCodecInfos.add(new VideoCodecInfo(name, MediaCodecUtils.getCodecProperties(type, true)));
                    }

                    supportedCodecInfos.add(new VideoCodecInfo(name, MediaCodecUtils.getCodecProperties(type, false)));
                }
            }
            return supportedCodecInfos.toArray(new VideoCodecInfo[0]);
        }
    }

    @Nullable
    private MediaCodecInfo findCodecForType(VideoCodecMimeType type) {
        for (int i = 0; i < MediaCodecList.getCodecCount(); ++i) {
            MediaCodecInfo info = null;

            try {
                info = MediaCodecList.getCodecInfoAt(i);
                System.out.println("findCodecForType111->type:"+type+",info:"+info.getName());
            } catch (IllegalArgumentException var5) {
                Log.e("H264", "H264HardwareVideoEncoderFactory : Cannot retrieve encoder codec info", var5);
            }

            if (info != null && info.isEncoder() && this.isSupportedCodec(info, type)) {
                System.out.println("findCodecForType222->type:"+type+",info:"+info.getName());
                return info;
            }
        }

        return null;
    }

    private boolean isSupportedCodec(MediaCodecInfo info, VideoCodecMimeType type) {
        if (!MediaCodecUtils.codecSupportsType(info, type)) {
            return false;
        } else if (MediaCodecUtils.selectColorFormat(MediaCodecUtils.ENCODER_COLOR_FORMATS, info.getCapabilitiesForType(type.mimeType())) == null) {
            return false;
        } else {
            return this.isHardwareSupportedInCurrentSdk(info, type) && this.isMediaCodecAllowed(info);
        }
    }

    private boolean isHardwareSupportedInCurrentSdk(MediaCodecInfo info, VideoCodecMimeType type) {
        switch (type) {
            case VP8:
                return this.isHardwareSupportedInCurrentSdkVp8(info);
            case VP9:
                return this.isHardwareSupportedInCurrentSdkVp9(info);
            case H264:
                return this.isHardwareSupportedInCurrentSdkH264(info);
            default:
                return false;
        }
    }

    private boolean isHardwareSupportedInCurrentSdkVp8(MediaCodecInfo info) {
        String name = info.getName();
        return name.startsWith("OMX.qcom.") && Build.VERSION.SDK_INT >= 19 || name.startsWith("OMX.Exynos.") && Build.VERSION.SDK_INT >= 23 || name.startsWith("OMX.Intel.") && Build.VERSION.SDK_INT >= 21 && this.enableIntelVp8Encoder;
    }

    private boolean isHardwareSupportedInCurrentSdkVp9(MediaCodecInfo info) {
        String name = info.getName();
        return (name.startsWith("OMX.qcom.") || name.startsWith("OMX.Exynos.")) && Build.VERSION.SDK_INT >= 24;
    }

    private boolean isHardwareSupportedInCurrentSdkH264(MediaCodecInfo info) {
        if (H264_HW_EXCEPTION_MODELS.contains(Build.MODEL)) {
            return false;
        } else {
            String name = info.getName();
            return name.startsWith("OMX.qcom.") && Build.VERSION.SDK_INT >= 19 || name.startsWith("OMX.Exynos.") && Build.VERSION.SDK_INT >= 21
                    || TextUtils.equals("OMX.rk.video_encoder.avc", name) && Build.VERSION.SDK_INT >= 21;//追加自己的H264 编译器
        }
    }

    private boolean isMediaCodecAllowed(MediaCodecInfo info) {
        return this.codecAllowedPredicate == null || this.codecAllowedPredicate.test(info);
    }

    private int getKeyFrameIntervalSec(VideoCodecMimeType type) {
        switch (type) {
            case VP8:
            case VP9:
                return 100;
            case H264:
                return 20;
            default:
                throw new IllegalArgumentException("Unsupported VideoCodecMimeType " + type);
        }
    }

    private int getForcedKeyFrameIntervalMs(VideoCodecMimeType type, String codecName) {
        if (type == VideoCodecMimeType.VP8 && codecName.startsWith("OMX.qcom.")) {
            if (Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22) {
                return 15000;
            }

            if (Build.VERSION.SDK_INT == 23) {
                return 20000;
            }

            if (Build.VERSION.SDK_INT > 23) {
                return 15000;
            }
        }

        return 0;
    }

    private BitrateAdjuster createBitrateAdjuster(VideoCodecMimeType type, String codecName) {
        if (codecName.startsWith("OMX.Exynos.")) {
            return (BitrateAdjuster) (type == VideoCodecMimeType.VP8 ? new DynamicBitrateAdjuster() : new FramerateBitrateAdjuster());
        } else {
            return new BaseBitrateAdjuster();
        }
    }

    private boolean isH264HighProfileSupported(MediaCodecInfo info) {
        return this.enableH264HighProfile && Build.VERSION.SDK_INT > 23 && info.getName().startsWith("OMX.Exynos.");
    }
}
