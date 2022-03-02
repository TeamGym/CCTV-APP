package kr.ac.kpu.cctvmanager.view

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.max
import kotlin.math.min


class GstSurfaceView(activity: Activity, savedInstanceState: Bundle?, var playUrl: String) : SurfaceView(activity),
    SurfaceHolder.Callback {
    var media_width = 640
    var media_height = 480
    private var isPlayingDesired = false

    init {
        Class.forName("org.freedesktop.gstreamer.GStreamer")
            .getDeclaredMethod("init", Context::class.java)
            .invoke(null, activity)
        holder.addCallback(this)
        isPlayingDesired = savedInstanceState?.getBoolean(PLAYING) ?: false
        initialize()
    }

    fun saveInstanceState(bundle: Bundle) {
        bundle.putBoolean(PLAYING, isPlayingDesired)
    }

    fun play() {
        Log.d("CCTVManager", "play")
        isPlayingDesired = true
        setPlayUrl0(playUrl)
        play0()
    }

    fun pause() {
        Log.d("CCTVManager", "pause")
        isPlayingDesired = false
        pause0()
    }

    fun destroy() {
        Log.d("CCTVManager", "destroy")
        destroy0()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 0
        var height = 0
        val wmode = MeasureSpec.getMode(widthMeasureSpec)
        val hmode = MeasureSpec.getMode(heightMeasureSpec)
        val wsize = MeasureSpec.getSize(widthMeasureSpec)
        val hsize = MeasureSpec.getSize(heightMeasureSpec)

        when (wmode) {
            MeasureSpec.AT_MOST -> {
                if (hmode == MeasureSpec.EXACTLY)
                    width = min(hsize * media_width / media_height, wsize)
            }
            MeasureSpec.EXACTLY -> width = wsize
            MeasureSpec.UNSPECIFIED -> width = media_width
        }

        when (hmode) {
            MeasureSpec.AT_MOST -> {
                if (wmode == MeasureSpec.EXACTLY)
                    height = min(wsize * media_height / media_width, hsize)
            }
            MeasureSpec.EXACTLY -> height = hsize
            MeasureSpec.UNSPECIFIED -> height = media_height
        }

        if (hmode == MeasureSpec.AT_MOST && wmode == MeasureSpec.AT_MOST) {
            val correct_height = width * media_height / media_width
            val correct_width = width * media_width / media_height

            if (correct_height < height)
                height = correct_height
            else
                width = correct_width
        }

        width = max(suggestedMinimumWidth, width)
        height = max(suggestedMinimumHeight, height)
        setMeasuredDimension(width, height)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d("CCTVManager", "surface changed to format=$format width=$width height=$height")
        surfaceInit0(holder.surface);
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("CCTVManager", "surface created: ${holder.surface}")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("CCTVManager", "surface destroyed")
        surfaceDestroy0()
    }

    fun onGstreamerInitialized() {
        if (isPlayingDesired)
            play0()
        else
            pause0()
    }

    private var nativeData: Long = 0
    private external fun initialize()
    private external fun play0()
    private external fun pause0()
    private external fun destroy0()
    private external fun setPlayUrl0(url: String)
    private external fun surfaceInit0(surface: Any)
    private external fun surfaceDestroy0()

    companion object {
        private const val PLAYING = "GstSurfaceView.playing"

        @JvmStatic
        private external fun initializeClass(): Boolean

        init {
            initializeClass()
        }
    }
}