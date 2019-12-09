package com.sd.lib.stream;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.sd.lib.stream.factory.DefaultStreamFactory;
import com.sd.lib.stream.factory.WeakCacheDefaultStreamFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 流管理类
 */
public class FStreamManager
{
    private static final FStreamManager INSTANCE = new FStreamManager();

    private FStreamManager()
    {
    }

    public static FStreamManager getInstance()
    {
        return INSTANCE;
    }

    private final Map<Class<? extends FStream>, List<FStream>> mMapStream = new ConcurrentHashMap<>();
    private final Map<FStream, StreamBinder> mMapStreamBinder = new WeakHashMap<>();

    private boolean mIsDebug;

    public boolean isDebug()
    {
        return mIsDebug;
    }

    public void setDebug(boolean debug)
    {
        mIsDebug = debug;
    }

    /**
     * {@link #bindStream(FStream, Activity)}
     */
    @Deprecated
    public void bindActivity(FStream stream, Activity target)
    {
        bindStream(stream, target);
    }

    /**
     * {@link #bindStream(FStream, View)}
     */
    @Deprecated
    public void bindView(FStream stream, View target)
    {
        bindStream(stream, target);
    }

    /**
     * {@link ActivityStreamBinder}
     *
     * @param stream
     * @param target
     */
    public synchronized void bindStream(FStream stream, Activity target)
    {
        if (target == null || !canBindStream(stream))
            return;

        unbindStreamInternal(stream);

        final ActivityStreamBinder binder = new ActivityStreamBinder(stream, target);
        if (binder.bind())
        {
            mMapStreamBinder.put(stream, binder);
            if (mIsDebug)
                Log.i(FStream.class.getSimpleName(), "bind activity stream:" + stream + " target:" + target);
        }
    }

    /**
     * {@link ViewStreamBinder}
     *
     * @param stream
     * @param target
     */
    public synchronized void bindStream(FStream stream, View target)
    {
        if (target == null || !canBindStream(stream))
            return;

        unbindStreamInternal(stream);

        final ViewStreamBinder binder = new ViewStreamBinder(stream, target);
        if (binder.bind())
        {
            mMapStreamBinder.put(stream, binder);
            if (mIsDebug)
                Log.i(FStream.class.getSimpleName(), "bind view stream:" + stream + " target:" + target);
        }
    }

    private boolean unbindStreamInternal(FStream stream)
    {
        final StreamBinder binder = mMapStreamBinder.remove(stream);
        if (binder != null)
        {
            binder.destroy();

            if (mIsDebug)
                Log.e(FStream.class.getSimpleName(), "bind destroyed stream:" + stream + " target:" + binder.getTarget());

            return true;
        }
        return false;
    }

    private boolean canBindStream(FStream stream)
    {
        final Class<? extends FStream>[] classes = getStreamClass(stream, true);
        return classes.length > 0;
    }

    /**
     * 注册流对象
     *
     * @param stream
     * @return 返回注册的接口
     */
    public synchronized Class<? extends FStream>[] register(FStream stream)
    {
        final StreamBinder binder = mMapStreamBinder.get(stream);
        if (binder != null)
            throw new IllegalArgumentException("stream has bound stream: " + stream + " target:" + binder.getTarget());

        return registerInternal(stream);
    }

    /**
     * 取消注册流对象
     *
     * @param stream
     */
    public synchronized void unregister(FStream stream)
    {
        if (unbindStreamInternal(stream))
            return;

        unregisterInternal(stream);
    }

    synchronized Class<? extends FStream>[] registerInternal(FStream stream)
    {
        final Class<? extends FStream>[] classes = getStreamClass(stream);
        for (Class<? extends FStream> item : classes)
        {
            List<FStream> holder = mMapStream.get(item);
            if (holder == null)
            {
                holder = new CopyOnWriteArrayList<>();
                mMapStream.put(item, holder);
            }

            if (!holder.contains(stream))
            {
                if (holder.add(stream))
                {
                    if (mIsDebug)
                        Log.i(FStream.class.getSimpleName(), "register:" + stream + " class:" + item.getName() + " count:" + (holder.size()));
                }
            }
        }
        return classes;
    }

    synchronized Class<? extends FStream>[] unregisterInternal(FStream stream)
    {
        final Class<? extends FStream>[] classes = getStreamClass(stream);
        for (Class<? extends FStream> item : classes)
        {
            final List<FStream> holder = mMapStream.get(item);
            if (holder == null)
                continue;

            if (holder.remove(stream))
            {
                if (mIsDebug)
                    Log.e(FStream.class.getSimpleName(), "unregister:" + stream + " class:" + item.getName() + " count:" + (holder.size()));

                if (holder.isEmpty())
                    mMapStream.remove(item);
            }
        }
        return classes;
    }

    private Class<? extends FStream>[] getStreamClass(FStream stream)
    {
        return getStreamClass(stream, false);
    }

    private Class<? extends FStream>[] getStreamClass(FStream stream, boolean getOne)
    {
        final Class<?> sourceClass = stream.getClass();
        checkProxyClass(sourceClass);

        final Set<Class<? extends FStream>> set = findAllStreamClass(sourceClass, getOne);
        return set.toArray(new Class[set.size()]);
    }

    private Set<Class<? extends FStream>> findAllStreamClass(Class<?> clazz, boolean getOne)
    {
        final Set<Class<? extends FStream>> set = new HashSet<>();

        while (true)
        {
            if (clazz == null)
                break;
            if (!FStream.class.isAssignableFrom(clazz))
                break;
            if (clazz.isInterface())
                throw new RuntimeException("clazz must not be an interface");

            for (Class<?> item : clazz.getInterfaces())
            {
                if (FStream.class.isAssignableFrom(item) && FStream.class != item)
                {
                    set.add((Class<? extends FStream>) item);

                    if (getOne && set.size() > 0)
                        return set;
                }
            }

            clazz = clazz.getSuperclass();
        }

        return set;
    }

    static final class ProxyInvocationHandler implements InvocationHandler
    {
        private final FStreamManager mManager;

        private final Class<? extends FStream> mClass;
        private final Object mTag;
        private final FStream.DispatchCallback mDispatchCallback;
        private final FStream.ResultFilter mResultFilter;

        public ProxyInvocationHandler(FStreamManager manager, FStream.ProxyBuilder builder)
        {
            mManager = manager;

            mClass = builder.mClass;
            mTag = builder.mTag;
            mDispatchCallback = builder.mDispatchCallback;
            mResultFilter = builder.mResultFilter;
        }

        private boolean checkTag(FStream stream)
        {
            final Object tag = stream.getTagForStream(mClass);
            if (mTag == tag)
                return true;

            return mTag != null && mTag.equals(tag);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            final String methodName = method.getName();
            final Class<?> returnType = method.getReturnType();

            final Class<?>[] parameterTypes = method.getParameterTypes();
            if ("getTagForStream".equals(methodName)
                    && parameterTypes.length == 1 && parameterTypes[0] == Class.class)
            {
                throw new RuntimeException(methodName + " method can not be called on proxy instance");
            }


            final boolean isVoid = returnType == void.class || returnType == Void.class;
            Object result = processMainLogic(isVoid, method, args);


            if (isVoid)
            {
                result = null;
            } else if (returnType.isPrimitive() && result == null)
            {
                if (boolean.class == returnType)
                    result = false;
                else
                    result = 0;

                if (mManager.isDebug())
                    Log.e(FStream.class.getSimpleName(), "return type:" + returnType + " but method result is null, so set to " + result);
            }

            if (mManager.isDebug() && !isVoid)
                Log.i(FStream.class.getSimpleName(), "notify final return:" + result);

            return result;
        }

        private Object processMainLogic(final boolean isVoid, final Method method, final Object[] args) throws Throwable
        {
            List<FStream> holder = mManager.mMapStream.get(mClass);

            if (mManager.isDebug())
                Log.i(FStream.class.getSimpleName(), "notify -----> " + method + " " + (args == null ? "" : Arrays.toString(args)) + " tag:" + mTag + " count:" + (holder == null ? 0 : holder.size()));

            if (holder == null)
            {
                final FStream stream = mManager.getDefaultStream(mClass);
                if (stream == null)
                {
                    return null;
                } else
                {
                    holder = new ArrayList<>(1);
                    holder.add(stream);
                    Log.i(FStream.class.getSimpleName(), "create default stream:" + stream + " for class:" + mClass.getName());
                }
            }

            final boolean filterResult = mResultFilter != null && !isVoid;
            final List<Object> listResult = filterResult ? new LinkedList<>() : null;

            Object result = null;
            int index = 0;
            for (FStream item : holder)
            {
                if (!checkTag(item))
                    continue;

                if (mDispatchCallback != null)
                {
                    if (mDispatchCallback.beforeDispatch(item, method, args))
                    {
                        if (mManager.isDebug())
                            Log.i(FStream.class.getSimpleName(), "notify broken before dispatch");
                        break;
                    }
                }

                final Object itemResult = method.invoke(item, args);

                if (mManager.isDebug())
                    Log.i(FStream.class.getSimpleName(), "notify index:" + index + " stream:" + item + (isVoid ? "" : (" return:" + itemResult)));

                result = itemResult;

                if (filterResult)
                    listResult.add(itemResult);

                if (mDispatchCallback != null)
                {
                    if (mDispatchCallback.afterDispatch(item, method, args, itemResult))
                    {
                        if (mManager.isDebug())
                            Log.i(FStream.class.getSimpleName(), "notify broken after dispatch");
                        break;
                    }
                }

                index++;
            }

            if (filterResult && !listResult.isEmpty())
            {
                result = mResultFilter.filter(method, args, listResult);

                if (mManager.isDebug())
                    Log.i(FStream.class.getSimpleName(), "filter result: " + result);
            }

            return result;
        }
    }

    //---------- default stream start ----------

    private final Map<Class<? extends FStream>, Class<? extends FStream>> mMapDefaultStreamClass = new ConcurrentHashMap<>();
    private DefaultStreamFactory mDefaultStreamFactory;

    /**
     * 注册默认的流接口实现类
     * <p>
     * {@link DefaultStreamFactory}
     *
     * @param clazz
     */
    public synchronized void registerDefaultStream(Class<? extends FStream> clazz)
    {
        checkProxyClass(clazz);
        checkFStreamClass(clazz);

        final Set<Class<? extends FStream>> set = findAllStreamClass(clazz, false);
        if (set.isEmpty())
            throw new IllegalArgumentException("stream class was not found in " + clazz);

        for (Class<? extends FStream> item : set)
        {
            mMapDefaultStreamClass.put(item, clazz);
        }
    }

    /**
     * 取消注册默认的流接口实现类
     *
     * @param clazz
     */
    public synchronized void unregisterDefaultStream(Class<? extends FStream> clazz)
    {
        checkProxyClass(clazz);
        checkFStreamClass(clazz);

        final Set<Class<? extends FStream>> set = findAllStreamClass(clazz, false);
        if (set.isEmpty())
            return;

        for (Class<? extends FStream> item : set)
        {
            mMapDefaultStreamClass.remove(item);
        }
    }

    /**
     * 设置{@link DefaultStreamFactory}
     *
     * @param defaultStreamFactory
     */
    public synchronized void setDefaultStreamFactory(DefaultStreamFactory defaultStreamFactory)
    {
        mDefaultStreamFactory = defaultStreamFactory;
    }

    private synchronized FStream getDefaultStream(Class<? extends FStream> clazz)
    {
        final Class<? extends FStream> defaultClass = mMapDefaultStreamClass.get(clazz);
        if (defaultClass == null)
            return null;

        if (mDefaultStreamFactory == null)
            mDefaultStreamFactory = new WeakCacheDefaultStreamFactory();

        final DefaultStreamFactory.CreateParam param = new DefaultStreamFactory.CreateParam(clazz, defaultClass);
        final FStream stream = mDefaultStreamFactory.create(param);
        if (stream == null)
            throw new RuntimeException(mDefaultStreamFactory + " create null for param:" + param);

        return stream;
    }

    //---------- default stream end ----------

    private static void checkProxyClass(Class<?> clazz)
    {
        if (Proxy.isProxyClass(clazz))
            throw new IllegalArgumentException("proxy class is not supported");
    }

    private static void checkFStreamClass(Class<?> clazz)
    {
        if (clazz == FStream.class)
            throw new IllegalArgumentException("class must not be " + FStream.class);
    }
}
