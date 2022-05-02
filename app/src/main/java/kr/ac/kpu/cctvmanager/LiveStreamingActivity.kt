package kr.ac.kpu.cctvmanager

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.ac.kpu.cctvmanager.databinding.ActivityLiveStreamingBinding

const val LIVE_STREAMING_SERVER_INFO = "kr.ac.kpu.cctvmanager.LIVE_STREAMING_SERVER_INFO"
const val LIVE_STREAMING_SERVER_DOMAIN = "kr.ac.kpu.cctvmanager.LIVE_STREAMING_SERVER_DOMAIN"

class LiveStreamingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveStreamingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_live_streaming)
        val serverInfo = intent.extras?.getString(LIVE_STREAMING_SERVER_INFO)!!

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.frameLayout, GridCamFragment.newInstance(serverInfo))
                .commit()
        }
    }

    override fun onStart() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        return super.onStart()
    }
}