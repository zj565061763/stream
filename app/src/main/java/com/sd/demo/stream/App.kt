package com.sd.demo.stream

import android.app.Application
import com.sd.lib.stream.DefaultStreamManager
import com.sd.lib.stream.FStreamManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 打开调试模式
        FStreamManager.isDebug = true

        // 注册默认的Stream
        DefaultStreamManager.register(DefaultFragmentCallback::class.java)
    }
}