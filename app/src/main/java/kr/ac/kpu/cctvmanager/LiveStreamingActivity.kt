package kr.ac.kpu.cctvmanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.ac.kpu.cctvmanager.databinding.ActivityLiveStreamingBinding

class LiveStreamingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveStreamingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_live_streaming)
        val serverInfo = intent.extras?.getString(CONST_SERVER_INFO)!!

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.frameLayout, GridCamFragment.newInstance(serverInfo))
                .commit()
        }
    }

    companion object {
        const val CONST_SERVER_INFO = "kr.ac.kpu.cctvmanager.CONST_SERVER_INFO"
        const val CONST_SERVER_DOMAIN = "kr.ac.kpu.cctvmanager.CONST_SERVER_DOMAIN"
    }
}