package com.sd.lib.stream;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class StickyInvokeManager
{
    private static StickyInvokeManager sInstance;

    public static StickyInvokeManager getInstance()
    {
        if (sInstance != null) return sInstance;
        synchronized (StickyInvokeManager.class)
        {
            if (sInstance == null)
                sInstance = new StickyInvokeManager();
            return sInstance;
        }
    }

    private StickyInvokeManager()
    {
    }

    /** 保存方法调用信息 */
    private final Map<Class<? extends FStream>, Map<Object, InvokeInfo>> mMapInvokeInfo = new HashMap<>();

    /** 代理对象数量 */
    private final Map<Class<? extends FStream>, Integer> mMapProxyCount = new HashMap<>();

    /**
     * 代理对象创建触发
     *
     * @param clazz
     */
    public synchronized void proxyCreated(Class<? extends FStream> clazz)
    {
        final Integer count = mMapProxyCount.get(clazz);
        if (count == null)
        {
            mMapProxyCount.put(clazz, 1);
        } else
        {
            mMapProxyCount.put(clazz, count + 1);
        }
    }

    /**
     * 代理对象销毁触发
     *
     * @param clazz
     */
    public synchronized void proxyDestroyed(Class<? extends FStream> clazz)
    {
        final Integer count = mMapProxyCount.get(clazz);
        if (count == null)
            throw new RuntimeException("count is null when destroy proxy:" + clazz.getName());

        final int targetCount = count - 1;
        if (targetCount <= 0)
        {
            mMapProxyCount.remove(clazz);
            mMapInvokeInfo.remove(clazz);
        } else
        {
            mMapProxyCount.put(clazz, targetCount);
        }
    }

    /**
     * 代理方法调用
     *
     * @param clazz
     * @param streamTag
     * @param method
     * @param args
     */
    public synchronized void proxyInvoke(Class<? extends FStream> clazz, Object streamTag, Method method, Object[] args)
    {
        if (args == null || args.length <= 0) return;
        if (!mMapProxyCount.containsKey(clazz)) return;

        Map<Object, InvokeInfo> holder = mMapInvokeInfo.get(clazz);
        if (holder == null)
        {
            holder = new HashMap<>();
            mMapInvokeInfo.put(clazz, holder);
        }

        InvokeInfo invokeInfo = holder.get(streamTag);
        if (invokeInfo == null)
        {
            invokeInfo = new InvokeInfo();
            holder.put(streamTag, invokeInfo);
        }
        invokeInfo.save(method, args);
    }

    private static final class InvokeInfo
    {
        private final Map<Method, Object[]> mMethodInfo = new HashMap<>();

        public void save(Method method, Object[] args)
        {
            mMethodInfo.put(method, args);
        }
    }
}
