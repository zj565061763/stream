package com.sd.lib.stream;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StickyInvokeManager {
    private static final String TAG = StickyInvokeManager.class.getSimpleName();

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
    void proxyCreated(Class<? extends FStream> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("null argument");
        }

        synchronized (clazz) {
            final Integer count = mMapProxyCount.get(clazz);
            if (count == null) {
                mMapProxyCount.put(clazz, 1);
            } else {
                mMapProxyCount.put(clazz, count + 1);
            }

            if (isDebug()) {
                Log.i(TAG, "+++++ proxyCreated"
                        + " class:" + clazz.getName()
                        + " count:" + mMapProxyCount.get(clazz)
                );
            }
        }
    }

    /**
     * 代理对象销毁触发
     *
     * @param clazz
     */
    void proxyDestroyed(Class<? extends FStream> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("null argument");
        }

        synchronized (clazz) {
            final Integer count = mMapProxyCount.get(clazz);
            if (count == null) {
                return;
            }

            final int targetCount = count - 1;
            if (targetCount <= 0) {
                mMapProxyCount.remove(clazz);
                mMapMethodInfo.remove(clazz);
            } else {
                mMapProxyCount.put(clazz, targetCount);
            }

            if (isDebug()) {
                Log.i(TAG, "----- proxyDestroyed"
                        + " class:" + clazz.getName()
                        + " count:" + mMapProxyCount.get(clazz)
                );
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
    void proxyInvoke(Class<? extends FStream> clazz, Object streamTag, Method method, Object[] args) {
        if (clazz == null) {
            throw new IllegalArgumentException("null argument");
        }

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

            if (isDebug()) {
                Log.i(TAG, "proxyInvoke"
                        + " class:" + clazz.getName()
                        + " tag:" + streamTag
                        + " method:" + method
                        + " args:" + Arrays.toString(args)
                );
            }
        }
    }

    boolean stickyInvoke(FStream stream, Class<? extends FStream> clazz) {
        if (!clazz.isAssignableFrom(stream.getClass())) {
            throw new IllegalArgumentException(clazz.getName() + " is not assignable from stream:" + stream);
        }

        synchronized (clazz) {
            final Map<Object, MethodInfo> holder = mMapMethodInfo.get(clazz);
            if (holder == null || holder.isEmpty()) {
                return false;
            }

            final Object streamTag = stream.getTagForStream(clazz);
            final MethodInfo methodInfo = holder.get(streamTag);
            if (methodInfo == null) {
                return false;
            }

            if (isDebug()) {
                Log.i(TAG, "stickyInvoke"
                        + " class:" + clazz.getName()
                        + " stream:" + stream
                        + " tag:" + streamTag
                );
            }

            try {
                methodInfo.invoke(stream, clazz);
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

        public void invoke(FStream stream, Class<? extends FStream> clazz) throws InvocationTargetException, IllegalAccessException {
            for (Map.Entry<Method, Object[]> item : mMethodInfo.entrySet()) {
                final Method method = item.getKey();
                final Object[] args = item.getValue();

                if (isDebug()) {
                    Log.i(TAG, "invoke"
                            + " class:" + clazz.getName()
                            + " stream:" + stream
                            + " method:" + method
                            + " args:" + Arrays.toString(args)
                    );
                }

                method.invoke(stream, args);
            }
        }
    }

    private static boolean isDebug() {
        return FStreamManager.getInstance().isDebug();
    }
}
