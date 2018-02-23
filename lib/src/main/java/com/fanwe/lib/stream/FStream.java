package com.fanwe.lib.stream;

/**
 * Created by zhengjun on 2018/2/9.
 */
public interface FStream
{
    default void register()
    {
        FStreamManager.getInstance().register(this);
    }

    default void unregister()
    {
        FStreamManager.getInstance().unregister(this);
    }

    default NotifySession getNotifySession()
    {
        final Class clazz = FStreamManager.getInstance().getStreamClass(this);
        return FStreamManager.getInstance().getNotifySession(clazz);
    }

    default Object getTag()
    {
        return null;
    }
}
