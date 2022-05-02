package kr.ac.kpu.cctvmanager

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.VideoView
import androidx.core.app.SharedElementCallback
import kr.ac.kpu.cctvmanager.databinding.FragmentDetailsCamBinding
import kr.ac.kpu.cctvmanager.databinding.FragmentGridCamBinding

private const val ARG_SERVER_ADDRESS = "kr.ac.kpu.cctvmanager.DetailsCamFragment.ARG_SERVER_ADDRESS"
private const val ARG_RTSP_URL = "kr.ac.kpu.cctvmanager.DetailsCamFragment.ARG_RTSP_URL"
private const val ARG_CAMERA_NAME = "kr.ac.kpu.cctvmanager.DetailsCamFragment.ARG_CAMERA_NAME"

class DetailsCamFragment : Fragment() {
    private lateinit var binding: FragmentDetailsCamBinding
    private var video: VideoView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailsCamBinding.inflate(inflater, container, false)

        binding.detailsLinearLayout.setBackgroundColor(0xffffffff.toInt())

        val serverAddress = arguments?.getString(ARG_SERVER_ADDRESS)
        val rtspUrl = arguments?.getString(ARG_RTSP_URL)
        val cameraName = arguments?.getString(ARG_CAMERA_NAME)
        //binding.detailsTextViewConnection.text = "Connected on $serverAddress"
        binding.detailsTextViewConnection.text = cameraName
        binding.detailsFrameLayout.transitionName = "cam_transition_shared"
        binding.detailsFrameLayout.setOnClickListener {
            requireActivity().onBackPressed()
        }

        video = VideoView(requireActivity())
        binding.detailsFrameLayout.addView(
            video,
            FrameLayout.LayoutParams( 0, 0, Gravity.CENTER))

        video?.setVideoURI(Uri.parse(rtspUrl))
        video?.start()

        setEnterSharedElementCallback(object: SharedElementCallback() {
            override fun onSharedElementStart(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                super.onSharedElementStart(
                    sharedElementNames,
                    sharedElements,
                    sharedElementSnapshots
                )

                video?.layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                    )
            }
        })

        setExitSharedElementCallback(object: SharedElementCallback() {
            override fun onSharedElementStart(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                super.onSharedElementStart(
                    sharedElementNames,
                    sharedElements,
                    sharedElementSnapshots
                )

                video?.layoutParams =
                    FrameLayout.LayoutParams(0, 0, Gravity.CENTER)
            }
        })

        /*
        val linearLayout = requireActivity().findViewById<LinearLayout>(R.id.linearLayout)

        val linearLayout1 = linearLayout.getChildAt(1) as LinearLayout
        val firstFrame = linearLayout1.getChildAt(0) as FrameLayout
        video = firstFrame.getChildAt(0) as VideoView

        val transition = firstFrame.layoutTransition
        firstFrame.layoutTransition = null

        firstFrame.removeView(video)
        binding.detailsFrameLayout.addView(
            video,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            ))

        firstFrame.layoutTransition = transition
         */

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        video?.resume()
    }

    override fun onStop() {
        super.onStop()

        video?.pause()
    }

    companion object {
        @JvmStatic
        fun newInstance(serverAddress: String, rtspUrl: String, cameraName: String) =
            DetailsCamFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SERVER_ADDRESS, serverAddress)
                    putString(ARG_RTSP_URL, rtspUrl)
                    putString(ARG_CAMERA_NAME, cameraName)
                }
            }
    }
}