package kr.ac.kpu.cctvmanager

import android.animation.LayoutTransition
import android.graphics.Color
import android.media.session.PlaybackState
import android.os.Bundle
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.beust.klaxon.Klaxon
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultAllocator
import kr.ac.kpu.cctvmanager.databinding.FragmentGridCamBinding
import kr.ac.kpu.cctvmanager.datatype.ServerInfo
import kr.ac.kpu.cctvmanager.receiver.TcpReceiver
import java.util.*
import kotlin.concurrent.timerTask


private const val TAG = "GridCamFragment"
private const val ARG_SERVER_INFO = "kr.ac.kpu.cctvmanager.GridCamFragment.ARG_SERVER_INFO"

class GridCamFragment : Fragment() {
    private lateinit var binding: FragmentGridCamBinding
    private lateinit var serverInfo: ServerInfo

    private var frameList: List<FrameLayout> = arrayListOf()
    private var receiverList: MutableList<TcpReceiver?> = mutableListOf(null, null, null, null)
    private var gridLayoutParamsList: MutableList<GridLayout.LayoutParams?> = mutableListOf(null, null, null, null)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGridCamBinding.inflate(inflater, container, false)

        binding.linearLayout.setBackgroundColor(0xffffffff.toInt())

        val serverInfoStr = arguments?.getString(ARG_SERVER_INFO)!!
        val serverAddress = requireActivity().intent.extras?.getString(LIVE_STREAMING_SERVER_DOMAIN)!!

        serverInfo = Klaxon().parse(serverInfoStr)!!
        serverInfo.address = serverAddress
        Log.d(TAG, "serverInfo = $serverInfo")

        binding.textViewConnection.text = "Connected on ${serverInfo.address}"

        frameList = (0..15).map { i ->
            val frame = FrameLayout(requireActivity())

            //val video = VideoView(requireActivity())
            //video.setMediaController(MediaController(requireActivity()))
            val video = PlayerView(requireActivity())
            video.z = 0.0f
            video.layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER)

            val label = TextView(requireActivity())
            label.setTextColor(0xffffffff.toInt())
            label.text = ""

            val status = TextView(requireActivity())
            status.setBackgroundColor(0xff000000.toInt())
            status.setTextColor(0xffffffff.toInt())
            status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f)
            status.textAlignment = View.TEXT_ALIGNMENT_CENTER
            status.gravity = Gravity.CENTER
            status.text = "Idle"

            frame.setPadding(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1.0f,
                    resources.displayMetrics).toInt())
            frame.setBackgroundColor(0xff000000.toInt())
            frame.background = ContextCompat.getDrawable(requireActivity(), R.drawable.layout_border_normal)
            frame.addView(video)
            frame.addView(status)
            frame.addView(label)
            /*
            frame.layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f)*/
            val gridLayoutParams =
                GridLayout.LayoutParams(
                    GridLayout.spec(i/4, 1, 1.0f),
                    GridLayout.spec(i%4, 1, 1.0f))
            gridLayoutParams.width = 0
            gridLayoutParams.height = 0
            frame.layoutParams = gridLayoutParams
            frame.z = 0.0f
            frame.transitionName = "cam_transition_$i"

            frame
        }

        val gridLayout = GridLayout(requireActivity())
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        layoutTransition.setDuration(200)
        gridLayout.layoutTransition = layoutTransition
        gridLayout.rowCount = 4
        gridLayout.columnCount = 4
        gridLayout.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        (0..15).map{ i ->
            gridLayout.addView(frameList[i])
        }
        binding.linearLayout.addView(gridLayout)

        serverInfo.cameraList.forEachIndexed { i, camInfo ->
            val url = "rtsp://${serverInfo.address}:${serverInfo.rtspPlayPort}/live/${camInfo.id}"
            camInfo.rtspUrl = url
            //val video = frameList[i].getChildAt(0) as VideoView
            val video = frameList[i].getChildAt(0) as PlayerView
            val status = frameList[i].getChildAt(1) as TextView
            val label = frameList[i].getChildAt(2) as TextView

            val text = SpannableString(camInfo.name)
            text.setSpan(ForegroundColorSpan(0xffffffff.toInt()), 0, text.length, 0)
            text.setSpan(BackgroundColorSpan(0x66000000.toInt()), 0, text.length, 0)
            label.setText(text, TextView.BufferType.SPANNABLE)

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setMaxPlaybackSpeed(1.5f)
                        .setMaxOffsetMs(2000)
                        .setTargetOffsetMs(500)
                        .build())
                .build()
            val mediaSource = RtspMediaSource.Factory()
                .setDebugLoggingEnabled(true)
                .createMediaSource(mediaItem)

            val player = ExoPlayer.Builder(requireActivity())
                    // don't use setMediaSourceFactory with MediaItem with liveConfiguration
                /*.setMediaSourceFactory(
                    DefaultMediaSourceFactory(requireActivity())
                        .setLiveTargetOffsetMs(100)
                        .setLiveMaxOffsetMs(500))*/
                /*.setLivePlaybackSpeedControl(
                    DefaultLivePlaybackSpeedControl.Builder()
                        .setFallbackMaxPlaybackSpeed(2.0f)
                        .build())*/
                .setLoadControl(CustomLoadControl())
                /*.setLoadControl(DefaultLoadControl.Builder()
                    .setAllocator(DefaultAllocator(true, 1024))
                    .setTargetBufferBytes(-1)
                    //.setBackBuffer(0, true)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .setBufferDurationsMs(
                        500,
                        500,
                        100,
                        100
                    )
                    .build())*/
                .build()
            //player.setMediaSource(mediaSource)
            player.setMediaItem(mediaItem)
            player.setPlaybackSpeed(1.1f)
            video.player = player
            video.controllerAutoShow = false
            video.useController = false
            video.hideController()
            //video.setShutterBackgroundColor(Color.TRANSPARENT)
            video.layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER)

            player.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    status.text = ""
                    status.visibility = View.INVISIBLE

                    val frameLayoutTransition = LayoutTransition()
                    frameLayoutTransition.enableTransitionType(LayoutTransition.CHANGING)
                    frameLayoutTransition.setDuration(200)
                    frameList[i].layoutTransition = layoutTransition

                    player.seekTo(0)
                    //player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    //player.seekTo(player.maxSeekToPreviousPosition - 500)
                    //player.pause()
                    //player.play()
                    Log.d(TAG, "maxSeekToPreviousPosition=${player.maxSeekToPreviousPosition}")
                    Log.d(TAG, "currentPosition=${player.currentPosition}")
                    Log.d(TAG, "currentLiveOffset=${player.currentLiveOffset}")
                    //player.seekToDefaultPosition()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                        Log.d(TAG, "player error PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW")
                        /*player.seekToDefaultPosition()
                        player.prepare()*/
                    } else {
                        error.printStackTrace()
                        Timer().schedule(timerTask {
                            activity?.runOnUiThread {
                                if (!player.isPlaying) {
                                    Log.d(TAG, "reconnecting... (camId=${camInfo.id})")
                                    val statusText = SpannableString("Connecting")
                                    statusText.setSpan(ForegroundColorSpan(0xccaaffaa.toInt()), 0, statusText.length, 0)
                                    statusText.setSpan(BackgroundColorSpan(0x66000000.toInt()), 0, statusText.length, 0)
                                    status.setText(statusText, TextView.BufferType.SPANNABLE)
                                    status.visibility = View.VISIBLE
                                    player.stop()
                                    player.prepare()
                                    player.playWhenReady = true
                                }
                            }
                        }, 5000)
                    }
                }
            })

            player.prepare()
            player.playWhenReady = true

            val statusText = SpannableString("Connecting")
            statusText.setSpan(ForegroundColorSpan(0xccaaffaa.toInt()), 0, statusText.length, 0)
            statusText.setSpan(BackgroundColorSpan(0x66000000.toInt()), 0, statusText.length, 0)
            status.setText(statusText, TextView.BufferType.SPANNABLE)

            frameList[i].setOnClickListener {
                if (gridLayoutParamsList[i] != null) {
                    frameList[i].layoutParams = gridLayoutParamsList[i]
                    frameList[i].z = 0.0f
                    video.z = 0.0f
                    frameList[1].getChildAt(0).visibility = View.VISIBLE
                    gridLayoutParamsList[i] = null
                } else {
                    gridLayoutParamsList[i] = frameList[i].layoutParams as GridLayout.LayoutParams
                    val gridLayoutParams =
                        GridLayout.LayoutParams(
                            GridLayout.spec(0, 2, 1.0f),
                            GridLayout.spec(0, 2, 1.0f))
                    gridLayoutParams.width = 0
                    gridLayoutParams.height = 0
                    frameList[i].z = 10.0f
                    video.z = 10.0f
                    frameList[1].getChildAt(0).visibility = View.INVISIBLE
                    frameList[i].layoutParams = gridLayoutParams
                }
            }

            /*
            val details = DetailsCamFragment.newInstance(serverInfo.address!!, url, camInfo.name)
            details.sharedElementEnterTransition = DetailsCamTransition()
            details.sharedElementReturnTransition = DetailsCamTransition()
            details.enterTransition = Fade()

            requireActivity().supportFragmentManager
                .beginTransaction()
                .add(R.id.frameLayout, details)
                .hide(details)
                .commit()

            // Reference: https://stackoverflow.com/questions/25151464/android-keep-fragment-running

            frameList[i].setOnClickListener {
                exitTransition = Fade()

                requireActivity().supportFragmentManager
                    .beginTransaction()
                    //.add(R.id.frameLayout, this)
                    .addSharedElement(frameList[i], "cam_transition_shared")
                    .hide(this)
                    .show(details)
                    .addToBackStack(null)
                    .commit()
                /*requireActivity().supportFragmentManager
                    .beginTransaction()
                    .addSharedElement(frameList[i], "cam_transition_shared")
                    .hide(this)
                    .show(details)
                    //.hide(this)
                    .addToBackStack(null)
                    .commit()*/
            }*/

            receiverList[i] = TcpReceiver(serverInfo, camInfo, frameList[i])
            receiverList[i]!!.start()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        frameList[0].let {
            val video = it.getChildAt(0) as PlayerView
            video.player?.play()
            /*
            video.requestFocus()
            video.setMediaController(MediaController(requireActivity()))
            video.start()*/
        }
/*
        frameList.forEach {
            Log.d(TAG, "resume")
            val video = it.getChildAt(0) as VideoView
            video.requestFocus()
            video.start()
            //video.resume()
        }
 */
}

    override fun onStop() {
        super.onStop()

        frameList.forEach {
            Log.d(TAG, "pause")
            val video = it.getChildAt(0) as PlayerView
            video.player?.pause()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(serverInfo: String) =
            GridCamFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SERVER_INFO, serverInfo)
                }
            }
    }
}