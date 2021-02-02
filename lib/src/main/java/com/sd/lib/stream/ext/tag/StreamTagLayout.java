package com.sd.lib.stream.ext.tag;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class StreamTagLayout extends FrameLayout implements StreamTagManager.IStreamTagView
{
    private final String mStreamTag;

    public StreamTagLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mStreamTag = createDefaultStreamTag(this);
    }

    @Override
    public String getStreamTag()
    {
        return mStreamTag;
    }

    private static String createDefaultStreamTag(Object object)
    {
        final String className = object.getClass().getName();
        final String hashCode = Integer.toHexString(System.identityHashCode(object));
        return className + "@" + hashCode;
    }
}
