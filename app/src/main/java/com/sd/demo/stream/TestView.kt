package com.sd.demo.stream

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager

class TestView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private val _callback: TestFragment.FragmentCallback = object : TestFragment.FragmentCallback {
        override fun getDisplayContent(): String {
            Log.i(TestView::class.java.simpleName, "getDisplayContent")
            return "TestView"
        }

        override fun getTagForStream(clazz: Class<out FStream?>): Any? {
            return null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        FStreamManager.bindStream(_callback, this)
    }
}