package com.sd.lib.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StreamConnection
{
    private final FStream mStream;
    private final FStreamManager mManager;
    private final Map<Class<? extends FStream>, ConnectionItem> mMapItem = new ConcurrentHashMap<>();

    StreamConnection(@NonNull FStream stream, @NonNull Class<? extends FStream>[] classes, @NonNull FStreamManager manager)
    {
        if (stream == null || classes == null || manager == null)
            throw new IllegalArgumentException("null argument");

        mStream = stream;
        mManager = manager;

        for (Class<? extends FStream> item : classes)
        {
            checkClassInterface(item);
            mMapItem.put(item, new ConnectionItem(item));
        }
    }

    /**
     * 返回优先级
     *
     * @param clazz
     * @return
     */
    int getPriority(@NonNull Class<? extends FStream> clazz)
    {
        checkClassInterface(clazz);
        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            return item.nPriority;
        return 0;
    }

    /**
     * 设置优先级
     *
     * @param priority
     */
    public void setPriority(int priority)
    {
        setPriority(priority, null);
    }

    /**
     * 设置优先级
     *
     * @param priority
     * @param clazz
     */
    public void setPriority(int priority, @Nullable Class<? extends FStream> clazz)
    {
        synchronized (mManager)
        {
            if (clazz == null)
            {
                for (ConnectionItem item : mMapItem.values())
                {
                    item.setPriority(priority);
                }
            } else
            {
                checkClassInterface(clazz);
                checkClassAssignable(clazz);

                final ConnectionItem item = mMapItem.get(clazz);
                if (item != null)
                    item.setPriority(priority);
            }
        }
    }

    /**
     * 停止分发
     *
     * @param clazz
     */
    public void breakDispatch(@NonNull Class<? extends FStream> clazz)
    {
        checkClassInterface(clazz);
        checkClassAssignable(clazz);

        synchronized (clazz)
        {
            final ConnectionItem item = mMapItem.get(clazz);
            if (item != null)
                item.breakDispatch();
        }
    }

    /**
     * 是否需要停止分发
     *
     * @param clazz
     * @return
     */
    boolean shouldBreakDispatch(@NonNull Class<? extends FStream> clazz)
    {
        checkClassInterface(clazz);
        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            return item.nShouldBreakDispatch;

        return false;
    }

    /**
     * 重置停止分发标志
     *
     * @param clazz
     */
    void resetBreakDispatch(@NonNull Class<? extends FStream> clazz)
    {
        checkClassInterface(clazz);
        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            item.resetBreakDispatch();
    }

    private void checkClassAssignable(@NonNull Class<? extends FStream> clazz)
    {
        if (!clazz.isAssignableFrom(mStream.getClass()))
            throw new IllegalArgumentException("class is not assignable from " + mStream.getClass().getName() + " class:" + clazz.getName());
    }

    private static void checkClassInterface(@NonNull Class<? extends FStream> clazz)
    {
        if (!clazz.isInterface())
            throw new IllegalArgumentException("class must be an interface class:" + clazz.getName());
    }

    private final class ConnectionItem
    {
        private final Class<? extends FStream> nClass;
        /** 优先级 */
        private volatile int nPriority;
        /** 是否停止分发 */
        private volatile boolean nShouldBreakDispatch;

        private ConnectionItem(@NonNull Class<? extends FStream> clazz)
        {
            if (clazz == null)
                throw new IllegalArgumentException("null argument");

            nClass = clazz;
        }

        /**
         * 设置优先级
         *
         * @param priority
         */
        public void setPriority(int priority)
        {
            if (nPriority != priority)
            {
                nPriority = priority;
                StreamConnection.this.onPriorityChanged(priority, mStream, nClass);
            }
        }

        /**
         * 设置停止分发
         */
        public void breakDispatch()
        {
            nShouldBreakDispatch = true;
        }

        /**
         * 重置停止分发标志
         */
        public void resetBreakDispatch()
        {
            nShouldBreakDispatch = false;
        }
    }

    protected abstract void onPriorityChanged(int priority, @NonNull FStream stream, @NonNull Class<? extends FStream> clazz);
}
