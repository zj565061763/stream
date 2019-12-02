package com.sd.lib.stream.factory;

import com.sd.lib.stream.FStream;

public abstract class CacheableDefaultStreamFactory extends SimpleDefaultStreamFactory
{
    @Override
    public FStream create(CreateParam param)
    {
        FStream stream = getCache(param);
        if (stream != null)
            return stream;

        stream = super.create(param);
        if (stream != null)
            setCache(param, stream);

        return stream;
    }

    protected abstract FStream getCache(CreateParam param);

    protected abstract void setCache(CreateParam param, FStream stream);
}
