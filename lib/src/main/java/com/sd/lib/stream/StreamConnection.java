package com.sd.lib.stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StreamConnection
{
    private final FStream mStream;
    private final Map<Class<? extends FStream>, ConnectionItem> mMapItem = new ConcurrentHashMap<>();

    StreamConnection(FStream stream, Class<? extends FStream>[] classes)
    {
        mStream = stream;
        if (classes != null && classes.length > 0)
        {
            for (Class<? extends FStream> item : classes)
            {
                getItem(item);
            }
        }
    }

    public final FStream getStream()
    {
        return mStream;
    }

    /**
     * 返回优先级
     *
     * @param clazz
     * @return
     */
    public synchronized int getPriority(Class<? extends FStream> clazz)
    {
        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            return item.nPriority;
        return 0;
    }

    /**
     * 设置优先级
     *
     * @param priority
     * @param clazz
     */
    public synchronized void setPriority(int priority, Class<? extends FStream> clazz)
    {
        if (clazz == null)
        {
            for (Map.Entry<Class<? extends FStream>, ConnectionItem> item : mMapItem.entrySet())
            {
                final ConnectionItem connectionItem = item.getValue();
                connectionItem.setPriority(priority);
            }
        } else
        {
            final ConnectionItem connectionItem = mMapItem.get(clazz);
            if (connectionItem != null)
                connectionItem.setPriority(priority);
        }
    }

    /**
     * 停止分发
     *
     * @param clazz
     */
    public synchronized void breakDispatch(Class<? extends FStream> clazz)
    {
        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            item.breakDispatch();
    }

    /**
     * 设置允许停止分发
     *
     * @param clazz
     */
    synchronized void enableBreakDispatch(Class<? extends FStream> clazz)
    {
        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            item.enableBreakDispatch();
    }

    /**
     * 是否需要停止分发
     *
     * @param clazz
     * @return
     */
    synchronized boolean shouldBreakDispatch(Class<? extends FStream> clazz)
    {
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
    synchronized void resetBreakDispatch(Class<? extends FStream> clazz)
    {
        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            item.resetBreakDispatch();
    }

    private synchronized ConnectionItem getItem(Class<? extends FStream> clazz)
    {
        if (!clazz.isAssignableFrom(mStream.getClass()))
            throw new IllegalArgumentException("clazz is not assignable from " + mStream.getClass().getName());

        ConnectionItem item = mMapItem.get(clazz);
        if (item == null)
        {
            item = new ConnectionItem(clazz);
            mMapItem.put(clazz, item);
        }
        return item;
    }

    private final class ConnectionItem
    {
        public final Class<? extends FStream> nClass;
        /** 优先级 */
        private volatile int nPriority;

        /** 是否允许停止分发 */
        private volatile boolean nEnableBreakDispatch;
        /** 是否停止分发 */
        private volatile boolean nShouldBreakDispatch;

        private ConnectionItem(Class<? extends FStream> clazz)
        {
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
         * 允许停止分发
         */
        public void enableBreakDispatch()
        {
            nEnableBreakDispatch = true;
        }

        /**
         * 设置停止分发
         */
        public void breakDispatch()
        {
            if (nEnableBreakDispatch)
                nShouldBreakDispatch = true;
        }

        /**
         * 重置停止分发标志
         */
        public void resetBreakDispatch()
        {
            nEnableBreakDispatch = false;
            nShouldBreakDispatch = false;
        }
    }

    protected abstract void onPriorityChanged(int priority, FStream stream, Class<? extends FStream> clazz);
}
