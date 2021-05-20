package com.sd.demo.stream

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.stream.TestFragment.FragmentCallback
import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 注册流对象
        FStreamManager.register(_callback1)

        // 注册流对象，并设置优先级，数值越大越先被通知，默认优先级：0
        FStreamManager.register(_callback2).setPriority(-1)

        // 绑定流对象，绑定之后会自动取消注册
        FStreamManager.bindStream(_callback3, this)
    }

    private val _callback1: FragmentCallback = object : FragmentCallback {
        override fun getDisplayContent(): String {
            return "1"
        }

        override fun getTagForStream(clazz: Class<out FStream?>): Any? {
            return null
        }
    }

    private val _callback2: FragmentCallback = object : FragmentCallback {
        override fun getDisplayContent(): String {
            return "2"
        }

        override fun getTagForStream(clazz: Class<out FStream?>): Any? {
            return null
        }
    }

    private val _callback3: FragmentCallback = object : FragmentCallback {
        override fun getDisplayContent(): String {
            return "3"
        }

        override fun getTagForStream(clazz: Class<out FStream?>): Any? {
            return null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        /**
         * 如果是手动调用[FStreamManager.register]注册的对象，需要取消注册
         */
        FStreamManager.unregister(_callback1)
        FStreamManager.unregister(_callback2)
    }
}