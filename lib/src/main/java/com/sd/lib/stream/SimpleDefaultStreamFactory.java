package com.sd.lib.stream;

public class SimpleDefaultStreamFactory implements FStreamManager.DefaultStreamFactory
{
    @Override
    public FStream create(Class<? extends FStream> classStream, Class<? extends FStream> classDefaultStream)
    {
        FStream stream = null;
        try
        {
            stream = classDefaultStream.newInstance();
        } catch (InstantiationException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        return stream;
    }
}
