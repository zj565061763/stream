package com.sd.lib.stream;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

class FStreamHolder
{
    private final Class<? extends FStream> mClass;
    private final FStreamManager mManager;
    private final Collection<FStream> mStreamHolder = new LinkedHashSet<>();

    private volatile boolean mIsNeedSort = false;

    public FStreamHolder(Class<? extends FStream> clazz, FStreamManager manager)
    {
        mClass = clazz;
        mManager = manager;
    }

    public boolean add(FStream stream)
    {
        if (stream == null)
            return false;

        final boolean result = mStreamHolder.add(stream);
        return result;
    }

    public boolean remove(FStream stream)
    {
        if (stream == null)
            return false;

        final boolean result = mStreamHolder.remove(stream);
        return result;
    }

    public int size()
    {
        return mStreamHolder.size();
    }

    public Collection<FStream> toCollection()
    {
        Collection<FStream> result = null;

        if (mIsNeedSort)
        {
            synchronized (mManager)
            {
                result = sort();
            }
            mIsNeedSort = false;
        } else
        {
            result = new ArrayList<>(mStreamHolder);
        }

        return result;
    }

    public void onPriorityChanged(int priority, FStream stream, Class<? extends FStream> clazz)
    {
        if (clazz != mClass)
            throw new IllegalArgumentException("expect class:" + mClass + " but class:" + clazz);

        mIsNeedSort = true;
    }

    private Collection<FStream> sort()
    {
        final List<FStream> listEntry = new ArrayList<>(mStreamHolder);
        Collections.sort(listEntry, new InternalStreamComparator());

        mStreamHolder.clear();
        mStreamHolder.addAll(listEntry);

        if (mManager.isDebug())
            Log.i(FStream.class.getSimpleName(), "sort stream for class:" + mClass.getName());

        return listEntry;
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
