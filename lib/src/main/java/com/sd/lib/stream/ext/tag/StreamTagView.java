package com.sd.lib.stream.ext.tag;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class StreamTagView extends FrameLayout implements StreamTagManager.IStreamTagView
{
    private final String mStreamTag;

    public StreamTagView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mStreamTag = createStreamTag(this);
    }

    @Override
    public String getStreamTag()
    {
        return mStreamTag;
    }

    /**
     * 创建tag
     *
     * @param object
     * @return
     */
    public static String createStreamTag(Object object)
    {
        final String className = object.getClass().getName();
        final String hashCode = Integer.toHexString(System.identityHashCode(object));
        return className + "@" + hashCode;
    }
}
