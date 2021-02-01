package com.sd.lib.stream.ext.tag;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class StreamTagLayout extends FrameLayout implements StreamTagManager.StreamTagHolder
{
    private volatile String mStreamTag;

    public StreamTagLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mStreamTag = createDefaultStreamTag(StreamTagLayout.this);
    }

    @Override
    public String getStreamTag()
    {
        return mStreamTag;
    }

    @Override
    public synchronized void setStreamTag(String tag)
    {
        mStreamTag = tag;
    }

    private static String createDefaultStreamTag(Object object)
    {
        final String className = object.getClass().getName();
        final String hashCode = Integer.toHexString(System.identityHashCode(object));
        return className + "@" + hashCode;
    }
}
