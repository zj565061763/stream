package com.sd.demo.stream

import com.sd.demo.stream.TestFragment.FragmentCallback
import com.sd.lib.stream.FStream

class DefaultFragmentCallback : FragmentCallback {
    override fun getDisplayContent(): String {
        return "default stream value"
    }

    override fun getTagForStream(clazz: Class<out FStream?>): Any? {
        return null
    }
}