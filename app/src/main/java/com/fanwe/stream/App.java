package com.fanwe.stream;

import android.app.Application;
import com.fanwe.lib.stream.FStreamManager;

/**
 * Created by Administrator on 2018/2/9.
 */

public class App extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        // 打开调试模式
        FStreamManager.getInstance().setDebug(true);
    }
}
