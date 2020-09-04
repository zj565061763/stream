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
    synchronized int getPriority(Class<? extends FStream> clazz)
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
    public synchronized void setPriority(int priority, Class<? extends FStream> clazz)
    {
        if (clazz == null)
        {
            for (Map.Entry<Class<? extends FStream>, ConnectionItem> item : mMapItem.entrySet())
            {
                item.getValue().setPriority(priority);
            }
        } else
        {
            checkClassInterface(clazz);
            checkClassAssignable(clazz);

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
        checkClassInterface(clazz);
        checkClassAssignable(clazz);

        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            item.breakDispatch();
    }

    /**
     * 是否需要停止分发
     *
     * @param clazz
     * @return
     */
    synchronized boolean shouldBreakDispatch(Class<? extends FStream> clazz)
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
    synchronized void resetBreakDispatch(Class<? extends FStream> clazz)
    {
        checkClassInterface(clazz);
        final ConnectionItem item = mMapItem.get(clazz);
        if (item != null)
            item.resetBreakDispatch();
    }

    private void checkClassAssignable(Class<? extends FStream> clazz)
    {
        if (!clazz.isAssignableFrom(mStream.getClass()))
            throw new IllegalArgumentException("class is not assignable from " + mStream.getClass().getName() + " class:" + clazz.getName());
    }

    private static void checkClassInterface(Class<? extends FStream> clazz)
    {
        if (!clazz.isInterface())
            throw new IllegalArgumentException("class must be an interface class:" + clazz.getName());
    }

    private final class ConnectionItem
    {
        public final Class<? extends FStream> nClass;
        /** 优先级 */
        private volatile int nPriority;
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

    protected abstract void onPriorityChanged(int priority, FStream stream, Class<? extends FStream> clazz);
}
