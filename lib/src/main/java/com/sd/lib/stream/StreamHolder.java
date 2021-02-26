package com.sd.lib.stream;

import android.util.Log;

import androidx.annotation.NonNull;

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

    /** 保存设置了优先级的流对象 */
    private final Map<FStream, Integer> mPriorityStreamHolder = new ConcurrentHashMap<>();

    private volatile boolean mIsPriorityChanged = false;
    private volatile boolean mHasDirtyStream = false;

    public StreamHolder(@NonNull Class<? extends FStream> clazz, @NonNull FStreamManager manager)
    {
        if (clazz == null || manager == null)
            throw new IllegalArgumentException("null argument");

        mClass = clazz;
        mManager = manager;
    }

    /**
     * 添加流对象
     *
     * @param stream
     * @return
     */
    public boolean add(@NonNull FStream stream)
    {
        if (stream == null)
            throw new IllegalArgumentException("null argument");

        final boolean result = mStreamHolder.add(stream);
        if (result)
        {
            if (!mPriorityStreamHolder.isEmpty())
            {
                // 如果之前已经有流对象设置了优先级，则添加新流对象的时候标记为需要重新排序
                mHasDirtyStream = true;
            }
        }
        return result;
    }

    /**
     * 移除流对象
     *
     * @param stream
     * @return
     */
    public boolean remove(@NonNull FStream stream)
    {
        if (stream == null)
            throw new IllegalArgumentException("null argument");

        final boolean result = mStreamHolder.remove(stream);
        mPriorityStreamHolder.remove(stream);

        return result;
    }

    public int size()
    {
        return mStreamHolder.size();
    }

    /**
     * 返回流集合
     *
     * @return
     */
    @NonNull
    public Collection<FStream> toCollection()
    {
        final boolean isNeedSort = mIsPriorityChanged || mHasDirtyStream;
        if (isNeedSort)
        {
            return sort();
        } else
        {
            return new ArrayList<>(mStreamHolder);
        }
    }

    @NonNull
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

    /**
     * 优先级变化
     *
     * @param priority
     * @param stream
     * @param clazz
     */
    public void onPriorityChanged(int priority, @NonNull FStream stream, @NonNull Class<? extends FStream> clazz)
    {
        if (stream == null || clazz == null)
            throw new IllegalArgumentException("null argument");

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
