package kr.ac.kpu.cctvmanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import kr.ac.kpu.cctvmanager.databinding.ActivityMainBinding
import kr.ac.kpu.cctvmanager.rsp.EndpointType
import kr.ac.kpu.cctvmanager.rsp.Request
import kr.ac.kpu.cctvmanager.rsp.RspConnection
import java.net.InetSocketAddress
import java.net.SocketException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val remoteIp = binding.editTextAddress.text.toString()
        val remotePort = binding.editTextPort.text.toString().toInt()

        val onException = RspConnection.EventHandler { event: RspConnection.ExceptionEvent ->
            runOnUiThread {
                binding.textError.text = event.throwable.stackTraceToString().lines()[0]
                binding.buttonLogin.isEnabled = true
            }
            event.throwable.printStackTrace()
        }

        binding.buttonLogin.setOnClickListener {
            binding.buttonLogin.isEnabled = false
            Thread {
                try {
                    val conn = RspConnection.makeConnection(
                        endpointType=EndpointType.APP,
                        remote=InetSocketAddress(remoteIp, remotePort),
                        eventHandlers=mapOf(
                            RspConnection.ExceptionEvent::class.java to
                                    mutableListOf(onException)
                        )
                    )
                    conn.sendRequest(Request(method=Request.Method.GET_INFO) { response ->
                        runOnUiThread {
                            binding.buttonLogin.isEnabled = true
                        }
                        val intent = Intent(this, LiveStreamingActivity::class.java)
                        startActivity(intent)
                    })
                } catch (e: SocketException) {
                    runOnUiThread {
                        binding.textError.text = e.stackTraceToString().lines()[0]
                        binding.buttonLogin.isEnabled = true
                    }
                    e.printStackTrace()
                }
            }.start()
        }
    }

    fun onException(event: RspConnection.ExceptionEvent) {
        runOnUiThread {
            binding.textError.text = event.throwable.stackTraceToString().lines()[0]
        }
        event.throwable.printStackTrace()
    }

    external fun getGstreamerVersion(): String

    companion object {
        init {
            System.loadLibrary("gstreamer_android")
            System.loadLibrary("cctvmanager_native")
        }
    }
}