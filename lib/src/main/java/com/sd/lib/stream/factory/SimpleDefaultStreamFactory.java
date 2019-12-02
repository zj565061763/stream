package com.sd.lib.stream.factory;

import com.sd.lib.stream.FStream;

public class SimpleDefaultStreamFactory implements DefaultStreamFactory
{
    @Override
    public FStream create(CreateParam param)
    {
        return newInstance(param);
    }

    protected FStream newInstance(CreateParam param)
    {
        try
        {
            return param.classDefaultStream.newInstance();
        } catch (InstantiationException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
