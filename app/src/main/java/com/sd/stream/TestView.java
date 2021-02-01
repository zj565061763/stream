package com.sd.stream;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.sd.lib.stream.FStream;
import com.sd.lib.stream.FStreamManager;

public class TestView extends FrameLayout
{
    public TestView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // 绑定流对象到当前View，会自动注册和取消注册
        FStreamManager.getInstance().bindStream(mFragmentCallback, this);
    }

    private final TestFragment.FragmentCallback mFragmentCallback = new TestFragment.FragmentCallback()
    {
        @Override
        public String getDisplayContent()
        {
            return "TestView";
        }

        @Override
        public Object getTagForStream(Class<? extends FStream> clazz)
        {
            return null;
        }
    };
}
