package com.sd.stream;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sd.lib.stream.FStream;
import com.sd.lib.stream.FStreamManager;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 添加TestFragment
        getSupportFragmentManager().beginTransaction().add(R.id.framelayout, new TestFragment()).commit();

        // 注册回调对象
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
        public String getDisplayContent()
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
        public String getDisplayContent()
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
        public String getDisplayContent()
        {
            return "3";
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // 要取消注册，否者流对象会一直被持有。
        FStreamManager.getInstance().unregister(mCallback1);
        FStreamManager.getInstance().unregister(mCallback2);
        FStreamManager.getInstance().unregister(mCallback3);
    }
}
