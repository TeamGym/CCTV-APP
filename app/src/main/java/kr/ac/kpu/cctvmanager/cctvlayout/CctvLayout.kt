package kr.ac.kpu.cctvmanager.cctvlayout

import android.app.Activity
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import kr.ac.kpu.cctvmanager.R
import java.net.SocketTimeoutException

private const val TAG = "CctvLayout"
class CctvLayout(activity: Activity) : FrameLayout(activity) {
    var isSetup = false

    private val surfaceView: UdpVideoView
    private val labelView: TextView
    private val statusView: TextView

    private val udpVideoPlayer: UdpVideoPlayer

    init {
        labelView = TextView(activity)
        labelView.setTextColor(0xffffffff.toInt())
        labelView.updatePadding(
            left=TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                5.0f,
                resources.displayMetrics).toInt())
        labelView.text = ""

        statusView = TextView(activity)
        statusView.setBackgroundColor(0xff000000.toInt())
        statusView.setTextColor(0xffffffff.toInt())
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
        statusView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        statusView.gravity = Gravity.CENTER
        statusView.text = "Idle"

        udpVideoPlayer = UdpVideoPlayer()
        surfaceView = UdpVideoView(activity, udpVideoPlayer)

        setPadding(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1.0f,
                resources.displayMetrics).toInt())
        setBackgroundColor(0xff000000.toInt())
        background = ContextCompat.getDrawable(activity, R.drawable.layout_border_normal)
        z = 0.0f

        addView(surfaceView, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER))
        addView(statusView, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT))
        addView(labelView, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT))

        udpVideoPlayer.callback = object : UdpVideoPlayer.Callback() {
            override fun onSizeChanged(width: Int, height: Int) {
                activity.runOnUiThread {
                    statusView.visibility = View.INVISIBLE
                    surfaceView.requestLayout()
                }
            }

            override fun onSocketTimeout(exception: SocketTimeoutException) {
                activity.runOnUiThread {
                    val status = SpannableString("Connecting")
                    status.setSpan(ForegroundColorSpan(0xccaaffaa.toInt()), 0, status.length, 0)
                    status.setSpan(BackgroundColorSpan(0x66000000.toInt()), 0, status.length, 0)
                    statusView.setText(status, TextView.BufferType.SPANNABLE)
                    statusView.visibility = View.VISIBLE
                }
            }

            override fun onConnectionResolved() {
                activity.runOnUiThread {
                    statusView.visibility = View.INVISIBLE
                }
            }
        }
    }

    fun getLabel() = labelView.text.toString()

    fun setLabel(label: String) {
        val text = SpannableString("$label ")
        text.setSpan(ForegroundColorSpan(0xffffffff.toInt()), 0, text.length, 0)
        //text.setSpan(BackgroundColorSpan(0x66000000.toInt()), 0, text.length, 0)
        labelView.setText(text, TextView.BufferType.SPANNABLE)
        labelView.setShadowLayer(2.0f, 1.4f, 1.3f, 0xff000000.toInt())
    }

    fun setup(port: Int) {
        val status = SpannableString("Standby")
        status.setSpan(ForegroundColorSpan(0xccaaffaa.toInt()), 0, status.length, 0)
        status.setSpan(BackgroundColorSpan(0x66000000.toInt()), 0, status.length, 0)
        statusView.setText(status, TextView.BufferType.SPANNABLE)
        statusView.visibility = View.VISIBLE

        udpVideoPlayer.port = port

        isSetup = true
    }

    fun play() {
        Log.d(TAG, "play")

        val status = SpannableString("Connecting")
        status.setSpan(ForegroundColorSpan(0xccaaffaa.toInt()), 0, status.length, 0)
        status.setSpan(BackgroundColorSpan(0x66000000.toInt()), 0, status.length, 0)
        statusView.setText(status, TextView.BufferType.SPANNABLE)
        statusView.visibility = View.VISIBLE

        udpVideoPlayer.playWhenReady()

        if (surfaceView.isActivated)
            udpVideoPlayer.onSurfaceChanged(surfaceView.holder)
    }

    fun pause() {
        Log.d(TAG, "pause")

        udpVideoPlayer.pause()
    }
}