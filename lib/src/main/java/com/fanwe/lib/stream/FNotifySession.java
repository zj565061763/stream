package com.fanwe.lib.stream;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by zhengjun on 2018/2/10.
 */
public final class FNotifySession
{
    private WeakReference<FStream> mStream;
    final Map<FStream, Object> MAP_RESULT = new WeakHashMap<>();

    void reset()
    {
        MAP_RESULT.clear();
        mStream = null;
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
