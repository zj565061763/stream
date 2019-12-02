package com.sd.lib.stream.factory;

import android.util.Log;

import com.sd.lib.stream.FStream;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * 用弱引用缓存流对象的工厂
 */
public class WeakCacheDefaultStreamFactory extends CacheableDefaultStreamFactory
{
    private final Map<Class<? extends FStream>, WeakReference<FStream>> mMapStream = new HashMap<>();
    private final ReferenceQueue<FStream> mReferenceQueue = new ReferenceQueue<>();

    private final Map<WeakReference<FStream>, Class<? extends FStream>> mMapStreamReverse = new HashMap<>();

    @Override
    protected FStream getCache(CreateParam param)
    {
        final WeakReference<FStream> reference = mMapStream.get(param.classStream);
        final FStream stream = reference == null ? null : reference.get();
        Log.i(WeakCacheDefaultStreamFactory.class.getSimpleName(), "getCache for class:" + param.classStream.getName() + " stream:" + stream + getSizeLog());
        return stream;
    }

    @Override
    protected void setCache(CreateParam param, FStream stream)
    {
        releaseReference();

        final WeakReference<FStream> reference = new WeakReference<>(stream, mReferenceQueue);
        mMapStream.put(param.classStream, reference);
        mMapStreamReverse.put(reference, param.classStream);
        Log.i(WeakCacheDefaultStreamFactory.class.getSimpleName(), "setCache for class:" + param.classStream.getName() + " stream:" + stream + getSizeLog());
    }

    private void releaseReference()
    {
        int count = 0;
        while (true)
        {
            final Reference<? extends FStream> reference = mReferenceQueue.poll();
            if (reference == null)
                break;

            final Class<? extends FStream> clazz = mMapStreamReverse.remove(reference);
            mMapStream.remove(clazz);

            count++;
        }

        if (count > 0)
        {
            Log.i(WeakCacheDefaultStreamFactory.class.getSimpleName(), "releaseReference count:" + count + getSizeLog());
        }
    }

    private String getSizeLog()
    {
        return "\r\n" + "size:" + mMapStream.size() + "," + mMapStreamReverse.size();
    }
}
