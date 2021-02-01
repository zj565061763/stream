package com.sd.stream;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sd.lib.stream.FStream;
import com.sd.lib.stream.FStreamManager;

public class TestView extends FrameLayout
{
    public TestView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        FStreamManager.getInstance().bindStream(mFragmentCallback, this);
    }

    private final TestFragment.FragmentCallback mFragmentCallback = new TestFragment.FragmentCallback()
    {
        @Override
        public String getDisplayContent()
        {
            return "testView";
        }

        @Override
        public Object getTagForStream(Class<? extends FStream> clazz)
        {
            return null;
        }
    };
}
