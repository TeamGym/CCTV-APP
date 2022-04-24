package kr.ac.kpu.cctvmanager

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.core.view.children
import com.beust.klaxon.Klaxon
import kr.ac.kpu.cctvmanager.databinding.FragmentGridCamBinding
import kr.ac.kpu.cctvmanager.datatype.ServerInfo
import kr.ac.kpu.cctvmanager.view.GstSurfaceView

private const val TAG = "GridCamFragment"
private const val ARG_SERVER_INFO = "ARG_SERVER_INFO"

class GridCamFragment : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentGridCamBinding
    private lateinit var serverInfo: ServerInfo

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGridCamBinding.inflate(inflater, container, false)

        val serverInfoStr = arguments?.getString(ARG_SERVER_INFO)!!
        serverInfo = Klaxon().parse<ServerInfo>(serverInfoStr)!!
        serverInfo.address = requireActivity().intent.extras?.getString(LIVE_STREAMING_SERVER_DOMAIN)!!
        serverInfo.cameraList.forEach {
            val url = "rtsp://${serverInfo.address}:${serverInfo.rtspPlayPort}/live/${it.id}"
            it.rtspUrl = url
            //binding.gridLayout.addView(GstSurfaceView(requireActivity(), serverInfo, it))
            Log.d(TAG, "Create GstSurfaceView (url=$url)")
            //val player = GstSurfaceView(requireActivity(), savedInstanceState, url)
            //val player = GstSurfaceView(requireActivity(), savedInstanceState, "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4")
            val player = VideoView(requireActivity())
            //player.setVideoPath(url)
            player.setVideoURI(Uri.parse(url))
            player.setMediaController(MediaController(requireActivity()))
            player.requestFocus()
            Log.d(TAG, "start")
            player.start()
            //player.holder.setFixedSize(640, 480) ////
            //binding.gridLayout.addView(player)
            binding.linearLayout.addView(player)
            //if (savedInstanceState != null)
            //    player.saveInstanceState(savedInstanceState)
        }
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //binding.gridLayout.children.forEach {
        /*binding.linearLayout.children.forEach {
            if (it !is GstSurfaceView)
                return
            it.saveInstanceState(outState)
        }*/
    }

    override fun onStart() {
        super.onStart()

        //binding.gridLayout.children.forEach {
        binding.linearLayout.children.forEach {
            if (it !is VideoView)
                return
            Log.d(TAG, "resume")
            it.resume()
        }
    }

    override fun onStop() {
        super.onStop()

        //binding.gridLayout.children.forEach {
        binding.linearLayout.children.forEach {
            if (it !is VideoView)
                return
            it.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //binding.gridLayout.children.forEach {
        /*binding.linearLayout.children.forEach {
            if (it !is GstSurfaceView)
                return
            it.destroy()
        }*/
    }

    override fun onClick(v: View) {/*
        val details = DetailsFragment.newInstance("a", "b")
        details.sharedElementEnterTransition = DetailsCamTransition()
        details.sharedElementReturnTransition = DetailsCamTransition()
        details.enterTransition = Fade()
        exitTransition = Fade()

        requireActivity().supportFragmentManager
            .beginTransaction()
            .addSharedElement(v, "sharedView")
            .replace(R.id.frameLayout, details)
            .addToBackStack(null)
            .commit()*/
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