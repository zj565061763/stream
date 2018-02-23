package com.fanwe.lib.stream;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by zhengjun on 2018/2/10.
 */
public final class NotifySession
{
    private WeakReference<FStream> mStream;
    private final Map<FStream, Object> MAP_RESULT = new WeakHashMap<>();

    void reset()
    {
        MAP_RESULT.clear();
        mStream = null;
    }

    void saveResult(FStream stream, Object result)
    {
        MAP_RESULT.put(stream, result);
    }

    Object getResult(FStream stream)
    {
        return MAP_RESULT.get(stream);
    }

    FStream getRequestAsResultStream()
    {
        return mStream == null ? null : mStream.get();
    }

    /**
     * 请求把对象当前的方法的返回值当做返回值
     *
     * @param stream
     */
    public void requestAsResult(FStream stream)
    {
        if (stream != null)
        {
            mStream = new WeakReference<>(stream);
        } else
        {
            mStream = null;
        }
    }
}
