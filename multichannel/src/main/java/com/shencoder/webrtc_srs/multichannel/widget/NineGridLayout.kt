package com.shencoder.webrtc_srs.multichannel.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import org.koin.core.component.KoinComponent
import org.webrtc.CameraVideoCapturer
import org.webrtc.PeerConnection
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoTrack


/**
 * 用于通话的GridLayout
 *
 * @author  ShenBen
 * @date    2021/9/8 14:05
 * @email   714081644@qq.com
 */

class PublishCallBean @JvmOverloads constructor(
    var callView: CallView? = null,
    var peerConnection: PeerConnection? = null,
    var videoTrack: VideoTrack?,
    var cameraVideoCapturer: CameraVideoCapturer? = null,
    var surfaceTextureHelper: SurfaceTextureHelper? = null
) {
    fun release() {
        cameraVideoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        videoTrack?.dispose()
        callView?.release()
        peerConnection?.dispose()
    }
}

class NineGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), KoinComponent {

    companion object {
        /**
         * 最大子view数量
         * view数量为1时： 1行x1列
         * view数量为2时： 1行x2列
         * view数量为3时： 2行x2列
         * view数量为4时： 2行x2列
         * view数量为5时： 2行x3列
         * view数量为6时： 2行x3列
         * view数量为7时： 3行x3列
         * view数量为8时： 3行x3列
         * view数量为9时： 3行x3列
         *
         * 子view的宽高强制均分
         */
        const val MAX_CHILD_COUNT = 9
    }


    /**
     * 子view宽度
     */
    private var mItemWidth = 0

    /**
     * 子view高度
     */
    private var mItemHeight = 0

    /**
     * 行数
     */
    private var mRowCount = 0

    /**
     * 列数
     */
    private var mColumnCount = 0

    /**
     * 分割线的宽度
     */
    @Px
    private var mDividerWidth = 15

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {
            throw RuntimeException("layout width or height can't be unspecified")
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        decideRowAndColumnCount()

        mItemWidth = (widthSize - paddingStart - paddingEnd) / mColumnCount
        mItemHeight = (heightSize - paddingTop - paddingBottom) / mRowCount
        println("onMeasure->widthSize:${widthSize},heightSize:${heightSize},widthMode:${widthMode},heightMode:${heightMode},width:${width},height:$height,childCount:$childCount,mItemWidth:${mItemWidth},mItemHeight:${mItemHeight}")

        var startX = paddingStart
        var startY = paddingTop

        val halfDividerWidth = mDividerWidth / 2

        val childCount = childCount
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val layoutParams = childView.layoutParams as LayoutParams


            //是否需要移动到下一行
            val isNeedMoveToNextLine = isNeedMoveToNextLine(i)

            if (isNeedMoveToNextLine) {
                startX = paddingStart
                startY += mItemHeight
            }

            layoutParams.mBorderRect.set(
                startX,
                startY,
                startX + mItemWidth,
                startY + mItemHeight
            )
            startX += mItemWidth

            //为了方便计算，实际的位置在边境位置上计算，顺带加上1/2分割线的宽度
            layoutParams.mDecorInsets.set(
                layoutParams.mBorderRect.left + layoutParams.leftMargin + halfDividerWidth,
                layoutParams.mBorderRect.top + layoutParams.topMargin + halfDividerWidth,
                layoutParams.mBorderRect.right - layoutParams.rightMargin - halfDividerWidth,
                layoutParams.mBorderRect.bottom - layoutParams.bottomMargin - halfDividerWidth
            )

            //是否在第一行，满足在加1/2分割线的宽度
            val isFirstLine = i / mColumnCount == 0
            //是否在最后一行，满足在加1/2分割线的宽度
            val isLastLine = i / mColumnCount == mRowCount - 1
            //是否在第一列，满足在加1/2分割线的宽度
            val isFirstColumn = i % mColumnCount == 0
            //是否在最后一列，满足在加1/2分割线的宽度
            val isLastColumn = i % mColumnCount == mColumnCount - 1

            if (isFirstLine) {
                layoutParams.mDecorInsets.top =
                    layoutParams.mDecorInsets.top + halfDividerWidth
            }
            if (isLastLine) {
                layoutParams.mDecorInsets.bottom =
                    layoutParams.mDecorInsets.bottom - halfDividerWidth
            }
            if (isFirstColumn) {
                layoutParams.mDecorInsets.left =
                    layoutParams.mDecorInsets.left + halfDividerWidth
            }
            if (isLastColumn) {
                layoutParams.mDecorInsets.right =
                    layoutParams.mDecorInsets.right - halfDividerWidth
            }

            //强制子view测量宽高
            childView.measure(
                MeasureSpec.makeMeasureSpec(layoutParams.mDecorInsets.width(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(layoutParams.mDecorInsets.height(), MeasureSpec.EXACTLY)
            )
        }

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childCount = childCount

        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val layoutParams = childView.layoutParams as LayoutParams
            val decorInsets = layoutParams.mDecorInsets
            childView.layout(
                decorInsets.left,
                decorInsets.top,
                decorInsets.right,
                decorInsets.bottom
            )
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return when (p) {
            is LayoutParams -> LayoutParams(p)
            is MarginLayoutParams -> LayoutParams(p)
            else -> LayoutParams(p)
        }
    }


    override fun addView(child: View?) {
        if (childCount >= MAX_CHILD_COUNT) {
            return
        }
        super.addView(child)
    }

    override fun addView(child: View?, index: Int) {
        if (childCount >= MAX_CHILD_COUNT) {
            return
        }
        super.addView(child, index)
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (childCount >= MAX_CHILD_COUNT) {
            return
        }
        super.addView(child, params)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount >= MAX_CHILD_COUNT) {
            return
        }
        super.addView(child, index, params)
    }

    override fun addView(child: View?, width: Int, height: Int) {
        if (childCount >= MAX_CHILD_COUNT) {
            return
        }
        super.addView(child, width, height)
    }

    override fun addViewInLayout(
        child: View?,
        index: Int,
        params: ViewGroup.LayoutParams?
    ): Boolean {
        if (childCount >= MAX_CHILD_COUNT) {
            return false
        }
        return super.addViewInLayout(child, index, params)
    }

    override fun addViewInLayout(
        child: View?,
        index: Int,
        params: ViewGroup.LayoutParams?,
        preventRequestLayout: Boolean
    ): Boolean {
        if (childCount >= MAX_CHILD_COUNT) {
            return false
        }
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }

    /**
     * @param position
     * @return 是否需要换到下一行
     */
    private fun isNeedMoveToNextLine(position: Int): Boolean {
        return position != 0 && position % mColumnCount == 0
    }

    /**
     * 根据[getChildCount]判断行数和列数
     */
    private fun decideRowAndColumnCount() {
        val childCount = childCount
        if (childCount > MAX_CHILD_COUNT) {
            mRowCount = 2
            mColumnCount = 2
            return
        }
        when (childCount) {
            0, 1 -> {
                mRowCount = 1
                mColumnCount = 1
            }
            2 -> {
                mRowCount = 1
                mColumnCount = 2
            }
            3, 4 -> {
                mRowCount = 2
                mColumnCount = 2
            }
            5, 6 -> {
                mRowCount = 2
                mColumnCount = 3
            }
            7, 8, 9 -> {
                mRowCount = 3
                mColumnCount = 3
            }
        }


    }

    open class LayoutParams : MarginLayoutParams {

        /**
         * view的边境位置
         * 不包括margin和[mDividerWidth]
         */
        val mBorderRect = Rect()

        /**
         * 用于实际绘制位置信息
         * 除去margin和[mDividerWidth]
         */
        val mDecorInsets = Rect()

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {}
        constructor(width: Int, height: Int) : super(width, height) {}
        constructor(source: MarginLayoutParams?) : super(source) {}
        constructor(source: ViewGroup.LayoutParams?) : super(source) {}
        constructor(source: LayoutParams?) : super(source as ViewGroup.LayoutParams?) {}
    }
}