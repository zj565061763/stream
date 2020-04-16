package com.sd.lib.stream.factory;

import android.util.Log;

import com.sd.lib.stream.FStream;
import com.sd.lib.stream.FStreamManager;

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

    private final Map<WeakReference<FStream>, Class<? extends FStream>> mMapReference = new HashMap<>();

    private boolean isDebug()
    {
        return FStreamManager.getInstance().isDebug();
    }

    @Override
    protected FStream getCache(CreateParam param)
    {
        final WeakReference<FStream> reference = mMapStream.get(param.classStream);
        final FStream stream = reference == null ? null : reference.get();

        if (isDebug())
            Log.i(WeakCacheDefaultStreamFactory.class.getSimpleName(), "getCache for class:" + param.classStream.getName() + " stream:" + stream + getSizeLog());

        return stream;
    }

    @Override
    protected void setCache(CreateParam param, FStream stream)
    {
        releaseReference();

        final WeakReference<FStream> reference = new WeakReference<>(stream, mReferenceQueue);
        final WeakReference<FStream> oldReference = mMapStream.put(param.classStream, reference);
        if (oldReference != null)
        {
            /**
             * 由于被回收的引用不一定会被及时的添加到ReferenceQueue中，
             * 所以这边判断一下旧的引用不为null的话，要移除掉
             */
            mMapReference.remove(oldReference);
        }

        mMapReference.put(reference, param.classStream);

        if (isDebug())
        {
            Log.i(WeakCacheDefaultStreamFactory.class.getSimpleName(), "+++++ setCache for class:" + param.classStream.getName() + " stream:" + stream + " reference:" + reference
                    + getSizeLog());
        }
    }

    private void releaseReference()
    {
        int count = 0;
        while (true)
        {
            final Reference<? extends FStream> reference = mReferenceQueue.poll();
            if (reference == null)
                break;

            final Class<? extends FStream> clazz = mMapReference.remove(reference);
            final WeakReference<FStream> streamReference = mMapStream.remove(clazz);

            if (streamReference == reference)
            {
                count++;
            } else
            {
                if (isDebug())
                {
                    Log.e(WeakCacheDefaultStreamFactory.class.getSimpleName(), "releaseReference"
                            + " class:" + clazz.getName()
                            + " reference:" + reference
                            + " streamReference:" + streamReference
                    );
                }
            }
        }

        if (count > 0)
        {
            if (isDebug())
                Log.i(WeakCacheDefaultStreamFactory.class.getSimpleName(), "releaseReference count:" + count + getSizeLog());
        }
    }

    private String getSizeLog()
    {
        return "\r\n" + "size:" + mMapStream.size() + "," + mMapReference.size();
    }
}
