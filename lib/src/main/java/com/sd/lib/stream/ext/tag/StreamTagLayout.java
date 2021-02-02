package com.sd.lib.stream.ext.tag;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class StreamTagLayout extends FrameLayout implements StreamTagManager.IStreamTagView
{
    private volatile String mStreamTag = StreamTagManager.STREAM_TAG_EMPTY;

    public StreamTagLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public String getStreamTag()
    {
        return mStreamTag;
    }

    @Override
    public void setStreamTag(String tag)
    {
        mStreamTag = tag;
    }
}
