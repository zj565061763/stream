package com.sd.demo.stream

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.sd.lib.stream.FStream

class TestFragment : Fragment() {

    /** 创建接口代理对象  */
    private val _callback = FStream.buildProxy(FragmentCallback::class.java) {
        /**
         * 设置代理对象的tag，默认tag为null
         * 注意：只有tag和当前代理对象tag相等的流对象才会被通知到
         */
        this.setTag(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val button = Button(context)
        button.text = "点击"
        button.setOnClickListener {
            // 获取显示内容
            button.text = _callback.getDisplayContent()
        }
        return button
    }

    interface FragmentCallback : FStream {
        fun getDisplayContent(): String
    }
}