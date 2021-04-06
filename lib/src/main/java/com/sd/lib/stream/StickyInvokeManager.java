package com.sd.lib.stream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StickyInvokeManager {
    private static StickyInvokeManager sInstance;

    public static StickyInvokeManager getInstance() {
        if (sInstance != null) {
            return sInstance;
        }

        synchronized (StickyInvokeManager.class) {
            if (sInstance == null) {
                sInstance = new StickyInvokeManager();
            }
            return sInstance;
        }
    }

    private StickyInvokeManager() {
    }

    /** 代理对象数量 */
    private final Map<Class<? extends FStream>, Integer> mMapProxyCount = new ConcurrentHashMap<>();
    /** 保存方法调用信息 */
    private final Map<Class<? extends FStream>, Map<Object, MethodInfo>> mMapMethodInfo = new ConcurrentHashMap<>();

    /**
     * 代理对象创建触发
     *
     * @param clazz
     */
    public void proxyCreated(Class<? extends FStream> clazz) {
        if (clazz == null) throw new IllegalArgumentException("null argument");

        synchronized (clazz) {
            final Integer count = mMapProxyCount.get(clazz);
            if (count == null) {
                mMapProxyCount.put(clazz, 1);
            } else {
                mMapProxyCount.put(clazz, count + 1);
            }
        }
    }

    /**
     * 代理对象销毁触发
     *
     * @param clazz
     */
    public void proxyDestroyed(Class<? extends FStream> clazz) {
        if (clazz == null) throw new IllegalArgumentException("null argument");

        synchronized (clazz) {
            final Integer count = mMapProxyCount.get(clazz);
            if (count == null)
                throw new RuntimeException("count is null when destroy proxy:" + clazz.getName());

            final int targetCount = count - 1;
            if (targetCount <= 0) {
                mMapProxyCount.remove(clazz);
                mMapMethodInfo.remove(clazz);
            } else {
                mMapProxyCount.put(clazz, targetCount);
            }
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
    public void proxyInvoke(Class<? extends FStream> clazz, Object streamTag, Method method, Object[] args) {
        if (clazz == null) throw new IllegalArgumentException("null argument");

        if (args == null || args.length <= 0) {
            // 参数为空，不保存
            return;
        }

        final Class<?> returnType = method.getReturnType();
        final boolean isVoid = returnType == void.class || returnType == Void.class;
        if (!isVoid) {
            // 方法有返回值，不保存
            return;
        }

        synchronized (clazz) {
            if (!mMapProxyCount.containsKey(clazz)) {
                return;
            }

            Map<Object, MethodInfo> holder = mMapMethodInfo.get(clazz);
            if (holder == null) {
                holder = new HashMap<>();
                mMapMethodInfo.put(clazz, holder);
            }

            MethodInfo methodInfo = holder.get(streamTag);
            if (methodInfo == null) {
                methodInfo = new MethodInfo();
                holder.put(streamTag, methodInfo);
            }
            methodInfo.save(method, args);
        }
    }

    public boolean stickyInvoke(FStream stream, Class<? extends FStream> clazz) {
        if (!clazz.isAssignableFrom(stream.getClass()))
            throw new IllegalArgumentException(clazz.getName() + " is not assignable from stream:" + stream);

        synchronized (clazz) {
            final Map<Object, MethodInfo> holder = mMapMethodInfo.get(clazz);
            if (holder == null || holder.isEmpty()) return false;

            final Object streamTag = stream.getTagForStream(clazz);
            final MethodInfo methodInfo = holder.get(streamTag);
            if (methodInfo == null) return false;

            try {
                methodInfo.invoke(stream);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class MethodInfo {
        private final Map<Method, Object[]> mMethodInfo = new ConcurrentHashMap<>();

        public void save(Method method, Object[] args) {
            mMethodInfo.put(method, args);
        }

        public void invoke(FStream stream) throws InvocationTargetException, IllegalAccessException {
            for (Map.Entry<Method, Object[]> item : mMethodInfo.entrySet()) {
                final Method method = item.getKey();
                final Object[] args = item.getValue();

                method.invoke(stream, args);
            }
        }
    }
}
