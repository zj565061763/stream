package com.sd.lib.stream.factory;

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
        return stream;
    }

    @Override
    protected void setCache(CreateParam param, FStream stream)
    {
        releaseReference();

        final WeakReference<FStream> reference = new WeakReference<>(stream, mReferenceQueue);
        mMapStream.put(param.classStream, reference);
        mMapStreamReverse.put(reference, param.classStream);
    }

    private void releaseReference()
    {
        while (true)
        {
            final Reference<? extends FStream> reference = mReferenceQueue.poll();
            if (reference == null)
                return;

            final Class<? extends FStream> clazz = mMapStreamReverse.remove(reference);
            mMapStream.remove(clazz);
        }
    }
}
