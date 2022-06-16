package kr.ac.kpu.cctvmanager.cctvlayout

import android.content.Context
import android.icu.util.Measure
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import java.lang.Integer.max
import java.lang.Integer.min

private const val TAG = "UdpVideoView"
class UdpVideoView(context: Context, val player: UdpVideoPlayer) : SurfaceView(context), SurfaceHolder.Callback {
    init {
        holder.addCallback(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        Log.d(TAG, "onMeasure width=$width, height=$height")

        val videoWidth = player.width
        val videoHeight = player.height

        Log.d(TAG, "onMeasure videoWidth=$videoWidth, videoHeight=$videoHeight")

        if (videoWidth == 1 || videoHeight == 1) {
            setMeasuredDimension(
                max(suggestedMinimumWidth, width),
                max(suggestedMinimumHeight, height))
            return
        }

        var calculatedWidth = when (widthMode) {
            MeasureSpec.AT_MOST -> {
                if (heightMode == MeasureSpec.EXACTLY)
                    min(height * videoWidth / videoHeight, width)
                else
                    width
            }
            MeasureSpec.EXACTLY -> width
            else -> videoWidth
        }

        var calculatedHeight = when (heightMode) {
            MeasureSpec.AT_MOST -> {
                if (widthMode == MeasureSpec.EXACTLY)
                    min(width * videoHeight / videoWidth, height)
                else
                    height
            }
            MeasureSpec.EXACTLY -> height
            else -> videoHeight
        }

        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            val w = height * videoWidth / videoHeight
            val h = width * videoHeight / videoWidth
            if (h < height)
                calculatedHeight = h
            else
                calculatedWidth = w
        }

        Log.d(TAG, "calculatedWidth=$calculatedWidth, calculatedHeight=$calculatedHeight")

        setMeasuredDimension(
            max(suggestedMinimumWidth, calculatedWidth),
            max(suggestedMinimumHeight, calculatedHeight))
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        player.onSurfaceChanged(holder)
    }
    override fun surfaceDestroyed(p0: SurfaceHolder) {}
}