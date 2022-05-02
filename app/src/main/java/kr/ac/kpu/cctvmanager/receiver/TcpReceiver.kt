package kr.ac.kpu.cctvmanager.receiver

import android.content.Context
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.UiContext
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import kr.ac.kpu.cctvmanager.R
import kr.ac.kpu.cctvmanager.datatype.CamInfo
import kr.ac.kpu.cctvmanager.datatype.DetectionResult
import kr.ac.kpu.cctvmanager.datatype.ServerInfo
import kr.ac.kpu.cctvmanager.datatype.VideoInfo
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset

private const val TAG = "TcpReceiver"
class TcpReceiver(private val serverInfo: ServerInfo, private val camInfo: CamInfo, private val videoFrame: FrameLayout) {
    private val thread: Thread
    private var resolution: VideoInfo? = null

    init {
        thread = Thread(::run)
    }

    fun start() {
        thread.start()
    }

    private fun run() {
        var count = 10
        while (count > 0) {
            try {
                Log.d(TAG, "connect to tcp server ${serverInfo.address}:${serverInfo.tcpPort} (count=$count)")
                val socket = Socket(serverInfo.address, serverInfo.tcpPort)
                socket.use {
                    Log.d(TAG, "successfully connected to tcp server ${serverInfo.address}:${serverInfo.tcpPort}")
                    it.outputStream.apply {
                        write("{\"mode\": 0, \"cam_id\": ${camInfo.id}}"
                            .toByteArray(Charset.forName("UTF-8")))
                        //write(camInfo.id.toString().toByteArray(Charset.forName("UTF-8")))
                        flush()
                    }
                    it.inputStream.bufferedReader(Charset.forName("UTF-8")).use { reader ->
                        resolution = Klaxon().parse<VideoInfo>(reader.readLine())
                        Log.d(TAG, "resolution: $resolution")
                        while (true) {
                            try {
                                val result = Klaxon().parse<DetectionResult>(reader.readLine())
                                Log.d(TAG, "result: ${result?.toString()}")
                                val video = videoFrame.getChildAt(0) as VideoView
                                val status = videoFrame.getChildAt(1) as TextView
                                if (result?.boxes?.fold(false) { acc, box ->
                                        acc || "person" == box.label // "gun" == box.label
                                    } == true) {
                                        Log.d(TAG, "gun detected (camId=${camInfo.id})")
                                    if (video.isPlaying) {
                                        val statusText = SpannableString("Gun Detected!!")
                                        statusText.setSpan(
                                            ForegroundColorSpan(0xccff6666.toInt()),
                                            0,
                                            statusText.length,
                                            0
                                        )
                                        statusText.setSpan(
                                            BackgroundColorSpan(0x66000000.toInt()),
                                            0,
                                            statusText.length,
                                            0
                                        )
                                        status.setText(
                                            statusText,
                                            TextView.BufferType.SPANNABLE
                                        )
                                        status.visibility = View.VISIBLE
                                    }
                                    videoFrame.background = ContextCompat.getDrawable(
                                        videoFrame.context,
                                        R.drawable.layout_border_warning
                                    )
                                } else {
                                    if (video.isPlaying) {
                                        status.text = ""
                                        status.visibility = View.INVISIBLE
                                    }
                                    videoFrame.background = ContextCompat.getDrawable(
                                        videoFrame.context,
                                        R.drawable.layout_border_normal
                                    )
                                }
                            } catch (e: KlaxonException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: KlaxonException) {
                throw e
            } finally {
                --count
                Thread.sleep(5000)
            }
        }
    }
}