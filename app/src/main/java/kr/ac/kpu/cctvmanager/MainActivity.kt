package kr.ac.kpu.cctvmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import kr.ac.kpu.cctvmanager.databinding.ActivityMainBinding
import kr.ac.kpu.cctvmanager.view.GstSurfaceView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var player: GstSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //binding.sampleText.text = nativeGetGstreamerInfo()
        player = GstSurfaceView(this, savedInstanceState, "rtsp://ipcam.stream:8554/bars")
        binding.linearLayout.addView(player)
        if (savedInstanceState != null)
            player.saveInstanceState(savedInstanceState)

        binding.buttonPlay.setOnClickListener {
            player.play()
        }
        binding.buttonPause.setOnClickListener {
            player.pause()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        player.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    external fun getGstreamerVersion(): String

    companion object {
        init {
            System.loadLibrary("gstreamer_android")
            System.loadLibrary("cctvmanager_native")
        }
    }
}