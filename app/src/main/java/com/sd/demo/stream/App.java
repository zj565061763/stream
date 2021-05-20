package com.sd.demo.stream;

import android.app.Application;

import com.sd.lib.stream.DefaultStreamManager;
import com.sd.lib.stream.FStreamManager;

/**
 * Created by Administrator on 2018/2/9.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 打开调试模式
        FStreamManager.INSTANCE.setDebug(true);

        // 注册默认的Stream
        DefaultStreamManager.INSTANCE.register(DefaultFragmentCallback.class);
    }
}
