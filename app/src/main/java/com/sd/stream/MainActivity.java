package com.sd.stream;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sd.lib.stream.FStream;
import com.sd.lib.stream.FStreamManager;
import com.sd.lib.stream.ext.tag.StreamTagLayout;

public class MainActivity extends AppCompatActivity
{
    private StreamTagLayout view_tag_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view_tag_layout = findViewById(R.id.view_tag_layout);
        view_tag_layout.setStreamTag(MainActivity.class.getSimpleName());

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
            return view_tag_layout.getStreamTag();
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
            return view_tag_layout.getStreamTag();
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
            return view_tag_layout.getStreamTag();
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
        /**
         * 取消注册
         * 不取消注册的话，流对象会一直被持有，此时流对象又持有其他UI资源对象的话，会内存泄漏
         */
        FStreamManager.getInstance().unregister(mCallback1);
        FStreamManager.getInstance().unregister(mCallback2);
        FStreamManager.getInstance().unregister(mCallback3);
    }
}
