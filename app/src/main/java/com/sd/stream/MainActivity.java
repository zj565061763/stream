package com.sd.stream;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sd.lib.stream.FStream;
import com.sd.lib.stream.FStreamManager;

public class MainActivity extends AppCompatActivity
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
        FStreamManager.getInstance().register(mCallback1);
        FStreamManager.getInstance().register(mCallback2);
        FStreamManager.getInstance().register(mCallback3);

    }

    private final TestFragment.FragmentCallback mCallback1 = new TestFragment.FragmentCallback()
    {
        @Override
        public Object getTagForStream(Class<? extends FStream> clazz)
        {
            return null;
        }

        @Override
        public String getActivityContent()
        {
            return "1";
        }
    };

    private final TestFragment.FragmentCallback mCallback2 = new TestFragment.FragmentCallback()
    {
        @Override
        public Object getTagForStream(Class<? extends FStream> clazz)
        {
            return null;
        }

        @Override
        public String getActivityContent()
        {
            return "2";
        }
    };

    private final TestFragment.FragmentCallback mCallback3 = new TestFragment.FragmentCallback()
    {
        @Override
        public Object getTagForStream(Class<? extends FStream> clazz)
        {
            return null;
        }

        @Override
        public String getActivityContent()
        {
            return "3";
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        /**
         * 取消注册
         * 不取消注册的话，流对象会一直被持有，此时流对象又持有其他UI资源对象的话，会内存泄漏
         */
        FStreamManager.getInstance().unregister(mCallback1);
        FStreamManager.getInstance().unregister(mCallback2);
        FStreamManager.getInstance().unregister(mCallback3);
    }
}
