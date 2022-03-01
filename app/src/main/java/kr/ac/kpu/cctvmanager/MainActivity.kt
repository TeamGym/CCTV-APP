package kr.ac.kpu.cctvmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import kr.ac.kpu.cctvmanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'cctvmanager' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'cctvmanager' library on application startup.
        init {
            System.loadLibrary("cctvmanager")
        }
    }
}