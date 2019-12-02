package com.sd.lib.stream;

public class SimpleDefaultStreamFactory implements FStreamManager.DefaultStreamFactory
{
    @Override
    public final FStream create(Class<? extends FStream> classStream, Class<? extends FStream> classDefaultStream)
    {
        final FStream stream = newInstance(classStream, classDefaultStream);
        return stream;
    }

    protected FStream newInstance(Class<? extends FStream> classStream, Class<? extends FStream> classDefaultStream)
    {
        try
        {
            return classDefaultStream.newInstance();
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
