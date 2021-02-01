package com.sd.lib.stream;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StreamHolder
{
    private final Class<? extends FStream> mClass;
    private final FStreamManager mManager;
    private final Collection<FStream> mStreamHolder = new LinkedHashSet<>();

    private final Map<FStream, Integer> mPriorityStreamHolder = new ConcurrentHashMap<>();

    private volatile boolean mIsPriorityChanged = false;
    private volatile boolean mHasDirtyStream = false;

    public StreamHolder(Class<? extends FStream> clazz, FStreamManager manager)
    {
        mClass = clazz;
        mManager = manager;
    }

    public boolean add(FStream stream)
    {
        if (stream == null)
            return false;

        final boolean result = mStreamHolder.add(stream);
        if (result)
        {
            final int priorityStreamSize = mPriorityStreamHolder.size();
            if (priorityStreamSize > 0)
                mHasDirtyStream = true;
        }
        return result;
    }

    public boolean remove(FStream stream)
    {
        if (stream == null)
            return false;

        final boolean result = mStreamHolder.remove(stream);
        mPriorityStreamHolder.remove(stream);

        return result;
    }

    public int size()
    {
        return mStreamHolder.size();
    }

    public Collection<FStream> toCollection()
    {
        if (isNeedSort())
        {
            return sort();
        } else
        {
            return new ArrayList<>(mStreamHolder);
        }
    }

    private boolean isNeedSort()
    {
        return mIsPriorityChanged || mHasDirtyStream;
    }

    private Collection<FStream> sort()
    {
        synchronized (mManager)
        {
            final List<FStream> listEntry = new ArrayList<>(mStreamHolder);
            Collections.sort(listEntry, new InternalStreamComparator());

            mStreamHolder.clear();
            mStreamHolder.addAll(listEntry);

            mIsPriorityChanged = false;
            mHasDirtyStream = false;

            if (mManager.isDebug())
                Log.i(FStream.class.getSimpleName(), "sort stream for class:" + mClass.getName());

            return listEntry;
        }
    }

    public void onPriorityChanged(int priority, FStream stream, Class<? extends FStream> clazz)
    {
        if (clazz != mClass)
            throw new IllegalArgumentException("expect class:" + mClass + " but class:" + clazz);

        if (priority == 0)
        {
            mPriorityStreamHolder.remove(stream);
        } else
        {
            mPriorityStreamHolder.put(stream, priority);
        }
        mIsPriorityChanged = true;

        if (mManager.isDebug())
        {
            Log.i(FStream.class.getSimpleName(), "onPriorityChanged"
                    + " priority:" + priority
                    + " clazz:" + clazz.getName()
                    + " priorityStreamHolder size:" + mPriorityStreamHolder.size()
                    + " stream:" + stream);
        }
    }

    private final class InternalStreamComparator implements Comparator<FStream>
    {
        @Override
        public int compare(FStream o1, FStream o2)
        {
            final StreamConnection o1Connection = mManager.getConnection(o1);
            final StreamConnection o2Connection = mManager.getConnection(o2);
            if (o1Connection != null && o2Connection != null)
            {
                return o2Connection.getPriority(mClass) - o1Connection.getPriority(mClass);
            }
            return 0;
        }
    }
}
