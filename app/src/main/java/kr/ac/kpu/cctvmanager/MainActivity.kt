package kr.ac.kpu.cctvmanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kr.ac.kpu.cctvmanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val intent = Intent(this, LiveStreamingActivity::class.java)
        startActivity(intent)
    }

    external fun getGstreamerVersion(): String

    companion object {
        init {
            System.loadLibrary("gstreamer_android")
            System.loadLibrary("cctvmanager_native")
        }
    }
}