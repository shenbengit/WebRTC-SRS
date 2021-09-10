package com.shencoder.webrtc_srs.multichannel.widget

import android.content.Context
import android.util.AttributeSet
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * 用于通话展示的view
 *
 * @author  ShenBen
 * @date    2021/9/7 15:12
 * @email   714081644@qq.com
 */
class CallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceViewRenderer(context, attrs) {


    override fun init(
        sharedContext: EglBase.Context?,
        rendererEvents: RendererCommon.RendererEvents?
    ) {
        super.init(sharedContext, rendererEvents)

    }
}