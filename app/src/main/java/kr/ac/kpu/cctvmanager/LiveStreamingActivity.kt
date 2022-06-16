package kr.ac.kpu.cctvmanager

import android.animation.LayoutTransition
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.core.view.setPadding
import androidx.databinding.DataBindingUtil
import kr.ac.kpu.cctvmanager.cctvlayout.CctvLayout
import kr.ac.kpu.cctvmanager.databinding.ActivityLiveStreamingBinding

class LiveStreamingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveStreamingBinding

    private var frameList: List<CctvLayout> = arrayListOf()
    private var layoutParamsList: MutableList<GridLayout.LayoutParams?> = MutableList(16) { null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_live_streaming)

        binding.linearLayout.setBackgroundColor(0xffffffff.toInt())
        binding.linearLayout.setPadding(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1.0f,
                resources.displayMetrics).toInt())

        frameList = (0..15).map { i ->
            val frame = CctvLayout(this)

            val layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(i/4*2, 2, 1.0f),
                GridLayout.spec(i%4*2, 2, 1.0f)
            )
            layoutParams.width = 0
            layoutParams.height = 0
            frame.layoutParams = layoutParams

            frame
        }

        val gridLayout = GridLayout(this)
        /*
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        layoutTransition.setDuration(200)
        gridLayout.layoutTransition = layoutTransition
         */
        gridLayout.rowCount = 8
        gridLayout.columnCount = 8
        gridLayout.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        (0..15).map{ i ->
            gridLayout.addView(frameList[i])
        }
        binding.linearLayout.addView(gridLayout)

        val i = 0
        frameList[i].setLabel("test")
        frameList[i].setup(50501)
        frameList[i].setOnClickListener {
            //frameList[i].layoutTransition = layoutTransition

            if (layoutParamsList[i] != null) {
                frameList[i].layoutParams = layoutParamsList[i]
                layoutParamsList[i] = null
                frameList[i].z = 0.0f
            } else {
                layoutParamsList[i] = frameList[i].layoutParams as GridLayout.LayoutParams
                val layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(i/4*2, gridLayout.rowCount - i/4*2, 1.0f),
                    GridLayout.spec(i%4*2, gridLayout.columnCount - i%4*2, 1.0f))
                layoutParams.width = 0
                layoutParams.height = 0
                frameList[i].z = 10.0f
                frameList[i].layoutParams = layoutParams
            }
        }
    }

    override fun onStart() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        frameList.forEach {
            if (it.isSetup)
                it.play()
        }

        return super.onStart()
    }

    override fun onStop() {
        super.onStop()

        frameList.forEach {
            if (it.isSetup)
                it.pause()
        }
    }

}