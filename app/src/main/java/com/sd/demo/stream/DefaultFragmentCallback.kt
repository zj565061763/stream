package com.sd.demo.stream

import com.sd.demo.stream.TestFragment.FragmentCallback
import com.sd.lib.stream.DefaultStreamManager
import com.sd.lib.stream.FStream

/**
 * 如果代理对象方法调用的时候未找到流对象，则会用注册的默认流Class创建流对象来触发
 *
 * [DefaultStreamManager.register]
 */
class DefaultFragmentCallback : FragmentCallback {
    override fun getDisplayContent(): String {
        return "default stream value"
    }

    override fun getTagForStream(clazz: Class<out FStream?>): Any? {
        return null
    }
}