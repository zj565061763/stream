package com.sd.stream;

import com.sd.lib.stream.FStream;

public class DefaultFragmentCallback implements TestFragment.FragmentCallback
{
    @Override
    public String getDisplayContent()
    {
        return "default stream value";
    }

    @Override
    public Object getTagForStream(Class<? extends FStream> clazz)
    {
        return null;
    }
}
