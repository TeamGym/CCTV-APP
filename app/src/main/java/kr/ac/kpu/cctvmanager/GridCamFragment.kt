package kr.ac.kpu.cctvmanager

import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.children
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import kr.ac.kpu.cctvmanager.databinding.FragmentGridCamBinding
import kr.ac.kpu.cctvmanager.datatype.ServerInfo
import kr.ac.kpu.cctvmanager.view.GstSurfaceView

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
        serverInfo.address = requireActivity().intent.extras?.getString(LiveStreamingActivity.CONST_SERVER_DOMAIN)!!
        serverInfo.cameraList.forEach {
            val url = "rtsp://${serverInfo.address}:${serverInfo.rtspPlayPort}/live/${it.id}"
            it.rtspUrl = url
            //binding.gridLayout.addView(GstSurfaceView(requireActivity(), serverInfo, it))
            val player = GstSurfaceView(requireActivity(), savedInstanceState, url)
            player.holder.setFixedSize(640, 480) ////
            binding.gridLayout.addView(player)
            if (savedInstanceState != null)
                player.saveInstanceState(savedInstanceState)
        }
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.gridLayout.children.forEach {
            if (it !is GstSurfaceView)
                return
            it.saveInstanceState(outState)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.gridLayout.children.forEach {
            if (it !is GstSurfaceView)
                return
            it.play()
        }
    }

    override fun onStop() {
        super.onStop()
        binding.gridLayout.children.forEach {
            if (it !is GstSurfaceView)
                return
            it.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.gridLayout.children.forEach {
            if (it !is GstSurfaceView)
                return
            it.destroy()
        }
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
        private const val ARG_SERVER_INFO = "ARG_SERVER_INFO"

        @JvmStatic
        fun newInstance(serverInfo: String) =
            GridCamFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SERVER_INFO, serverInfo)
                }
            }
    }
}