package kr.ac.kpu.cctvmanager.cctvlayout

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.IllegalStateException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

private const val TAG = "UdpVideoPlayer"
class UdpVideoPlayer {
    var port: Int? = null
    var socketTimeout = 3000
    var isPlayWhenReady = false

    var isPlaying = false
        private set

    var callback = Callback()

    var width = 1
        private set
    var height = 1
        private set

    private var socketThread: Thread? = null
    private var codecThread: Thread? = null

    private val frameQueue = ArrayBlockingQueue<ByteArray>(15)

    private val codec: MediaCodec = MediaCodec.createDecoderByType("video/avc")
    private var isConfigured = false
    private var lastKeyFrame: ByteArray? = null

    private fun runSocket() {
        val port = port ?: throw IllegalStateException()

        val socket = DatagramSocket(port)
        socket.soTimeout = socketTimeout
        socket.use {
            var isTimedOut = true

            val buffer = ByteArray(131072)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isPlaying) {
                try {
                    socket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "Socket timed out")
                    isTimedOut = true
                    callback.onSocketTimeout(e)
                    continue
                }

                if (isTimedOut) {
                    isTimedOut = false
                    callback.onConnectionResolved()
                }

                val bufferCopied = buffer.copyOfRange(0, packet.length)
                try {
                    frameQueue.add(bufferCopied)
                } catch (e: IllegalStateException) {
                    //Log.d(TAG, "queue full, rendering lag expected")

                    var previousFrame: ByteArray? = null
                    for (frame in frameQueue) {
                        if (frame[5] == 0x10.toByte()) {
                            val previousFrame = previousFrame ?: continue
                            previousFrame.let {
                                frameQueue.remove(it)
                            }
                        }
                        previousFrame = frame
                    }

                    if (frameQueue.remainingCapacity() == 0)
                        frameQueue.poll()
                    frameQueue.add(bufferCopied)
                }
            }
        }
        callback.onSocketClose()
    }

    private fun runCodec() {
        codec.start()
        Log.d(TAG, "codec started")

        var isConfigSet = false
        var buffer: ByteArray

        frameQueue.clear()

        while (isPlaying) {
            if (!isConfigSet && lastKeyFrame != null) {
                buffer = lastKeyFrame?.copyOf()!!
            } else {
                buffer = frameQueue.take()

                if (buffer[5] == 0x10.toByte())
                    lastKeyFrame = buffer.copyOf()
                else if (!isConfigSet)
                    continue
            }

            try {
                val inputBufferId = codec.dequeueInputBuffer(5000)
                if (inputBufferId >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)!!
                    inputBuffer.put(buffer, 0, buffer.size)
                    // timestamp == 0
                    codec.queueInputBuffer(inputBufferId, 0, buffer.size, 0, 0)
                }

                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 5000)
                if (outputBufferId >= 0) {
                    //val outputBuffer = codec.getOutputBuffer((outputBufferId))
                    codec.releaseOutputBuffer(outputBufferId, true)
                }

                if (!isConfigSet) {
                    callback.onFirstFrameRendered()
                    isConfigSet = true
                }

                val outputWidth = codec.outputFormat.getInteger("width")
                val outputHeight = codec.outputFormat.getInteger("height")
                if (outputWidth != width || outputHeight != height) {
                    width = outputWidth
                    height = outputHeight
                    callback.onSizeChanged(width, height)
                }
            } catch (e: IllegalStateException) {
                Log.d(TAG, e.message.toString())
                return
            }
        }

        codec.stop()
        isConfigured = false
        callback.onCodecStop()
    }

    fun onSurfaceChanged(holder: SurfaceHolder) {
        if (isConfigured)
            return

        codec.reset()
        codec.configure(
            MediaFormat.createVideoFormat("video/avc", 1, 1)
            , holder.surface, null, 0)
        Log.d(TAG, "codec configured")
        if (isPlayWhenReady)
            play()
        isConfigured = true
    }

    fun playWhenReady() {
        if (isConfigured)
            play()

        isPlayWhenReady = true
    }

    fun play() {
        assert(port != null)
        assert(!isPlaying)

        isPlaying = true
        socketThread = Thread(::runSocket)
        codecThread = Thread(::runCodec)
        socketThread?.start()
        codecThread?.start()
    }

    fun pause() {
        isPlaying = false
        isConfigured = false
        isPlayWhenReady = false
    }

    open class Callback {
        open fun onFirstFrameRendered() {}
        open fun onSizeChanged(width: Int, height: Int) {}
        open fun onSocketTimeout(exception: SocketTimeoutException) {}
        open fun onConnectionResolved() {}
        open fun onCodecStop() {}
        open fun onSocketClose() {}
    }
}
