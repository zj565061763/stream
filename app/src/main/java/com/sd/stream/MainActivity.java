package com.sd.stream;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sd.lib.stream.FStreamManager;

public class MainActivity extends AppCompatActivity implements TestFragment.FragmentCallback
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * 添加TestFragment
         */
        getSupportFragmentManager().beginTransaction().add(R.id.framelayout, new TestFragment()).commit();
        /**
         * 注册回调对象
         */
        FStreamManager.getInstance().register(this);

    }

    @Override
    public String getActivityContent()
    {
        return "MainActivity";
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        /**
         * 取消注册
         * 不取消注册的话，流对象会一直被持有，此时流对象又持有其他UI资源对象的话，会内存泄漏
         */
        FStreamManager.getInstance().unregister(this);
    }
}
