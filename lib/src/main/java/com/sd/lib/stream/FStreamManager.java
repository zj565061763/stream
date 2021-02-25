package com.sd.lib.stream;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sd.lib.stream.factory.DefaultStreamFactory;
import com.sd.lib.stream.factory.WeakCacheDefaultStreamFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<Class<? extends FStream>, StreamHolder> mMapStream = new ConcurrentHashMap<>();
    private final Map<FStream, StreamBinder> mMapStreamBinder = new WeakHashMap<>();

    private final Map<FStream, InternalStreamConnection> mMapStreamConnection = new ConcurrentHashMap<>();

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
    public void bindActivity(@NonNull FStream stream, @NonNull Activity target)
    {
        bindStream(stream, target);
    }

    /**
     * {@link #bindStream(FStream, View)}
     */
    @Deprecated
    public void bindView(@NonNull FStream stream, @NonNull View target)
    {
        bindStream(stream, target);
    }

    /**
     * {@link ActivityStreamBinder}
     *
     * @param stream
     * @param target
     * @return true-绑定成功或者已绑定；false-绑定失败
     */
    public synchronized boolean bindStream(@NonNull FStream stream, @NonNull Activity target)
    {
        if (stream == null || target == null)
            throw new IllegalArgumentException("null argument");

        final StreamBinder oldBinder = mMapStreamBinder.get(stream);
        if (oldBinder != null)
        {
            if (oldBinder.getTarget() == target)
            {
                //  已经绑定过了
                return true;
            } else
            {
                // target发生变化，先取消绑定
                unbindStream(stream);
            }
        }

        final ActivityStreamBinder binder = new ActivityStreamBinder(stream, target);
        if (binder.bind())
        {
            mMapStreamBinder.put(stream, binder);

            if (mIsDebug)
            {
                Log.i(FStream.class.getSimpleName(), "bind activity"
                        + " stream:" + stream
                        + " target:" + target
                        + " size:" + mMapStreamBinder.size());
            }

            return true;
        }
        return false;
    }

    /**
     * {@link ViewStreamBinder}
     *
     * @param stream
     * @param target
     * @return true-绑定成功或者已绑定；false-绑定失败
     */
    public synchronized boolean bindStream(@NonNull FStream stream, @NonNull View target)
    {
        if (stream == null || target == null)
            throw new IllegalArgumentException("null argument");

        final StreamBinder oldBinder = mMapStreamBinder.get(stream);
        if (oldBinder != null)
        {
            if (oldBinder.getTarget() == target)
            {
                //  已经绑定过了
                return true;
            } else
            {
                // target发生变化，先取消绑定
                unbindStream(stream);
            }
        }

        final ViewStreamBinder binder = new ViewStreamBinder(stream, target);
        if (binder.bind())
        {
            mMapStreamBinder.put(stream, binder);

            if (mIsDebug)
            {
                Log.i(FStream.class.getSimpleName(), "bind view"
                        + " stream:" + stream
                        + " target:" + target
                        + " size:" + mMapStreamBinder.size());
            }

            return true;
        }
        return false;
    }

    /**
     * 解绑并取消注册
     *
     * @param stream
     * @return
     */
    public synchronized boolean unbindStream(@NonNull FStream stream)
    {
        if (stream == null)
            throw new IllegalArgumentException("null argument");

        final StreamBinder binder = mMapStreamBinder.remove(stream);
        if (binder == null)
            return false;

        binder.destroy();
        if (mIsDebug)
        {
            Log.i(FStream.class.getSimpleName(), "unbind"
                    + " stream:" + stream
                    + " target:" + binder.getTarget()
                    + " size:" + mMapStreamBinder.size());
        }
        return true;
    }

    private void checkHasBound(@NonNull FStream stream)
    {
        if (stream == null)
            throw new IllegalArgumentException("null argument");

        final StreamBinder binder = mMapStreamBinder.get(stream);
        if (binder != null)
            throw new IllegalArgumentException("stream has bound. stream: " + stream + " target:" + binder.getTarget());
    }

    /**
     * 注册流对象
     *
     * @param stream
     * @return null-注册失败
     */
    @NonNull
    public synchronized StreamConnection register(@NonNull FStream stream)
    {
        checkHasBound(stream);
        return registerInternal(stream);
    }

    /**
     * 取消注册流对象
     *
     * @param stream
     */
    public synchronized void unregister(@NonNull FStream stream)
    {
        checkHasBound(stream);
        unregisterInternal(stream);
    }

    @NonNull
    synchronized StreamConnection registerInternal(@NonNull FStream stream)
    {
        if (stream == null)
            throw new IllegalArgumentException("null argument");

        InternalStreamConnection streamConnection = mMapStreamConnection.get(stream);
        if (streamConnection != null)
        {
            // 已经注册过了
            return streamConnection;
        }

        final Class<? extends FStream>[] classes = getStreamClass(stream);
        for (Class<? extends FStream> item : classes)
        {
            StreamHolder holder = mMapStream.get(item);
            if (holder == null)
            {
                holder = new StreamHolder(item, FStreamManager.this);
                mMapStream.put(item, holder);
            }

            if (holder.add(stream))
            {
                if (mIsDebug)
                {
                    Log.i(FStream.class.getSimpleName(), "+++++ register"
                            + " class:" + item.getName()
                            + " stream:" + stream
                            + " count:" + (holder.size()));
                }
            }
        }

        streamConnection = new InternalStreamConnection(stream, classes);
        mMapStreamConnection.put(stream, streamConnection);
        return streamConnection;
    }

    synchronized void unregisterInternal(@NonNull FStream stream)
    {
        if (stream == null)
            throw new IllegalArgumentException("null argument");

        final StreamConnection streamConnection = mMapStreamConnection.remove(stream);
        if (streamConnection == null)
            return;

        final Class<? extends FStream>[] classes = getStreamClass(stream);
        for (Class<? extends FStream> item : classes)
        {
            final StreamHolder holder = mMapStream.get(item);
            if (holder == null)
                continue;

            if (holder.remove(stream))
            {
                if (holder.size() <= 0)
                    mMapStream.remove(item);

                if (mIsDebug)
                {
                    Log.i(FStream.class.getSimpleName(), "----- unregister"
                            + " class:" + item.getName()
                            + " stream:" + stream
                            + " count:" + (holder.size()));
                }
            }
        }
    }

    /**
     * 返回流对象连接对象
     *
     * @param stream
     * @return
     */
    @Nullable
    public StreamConnection getConnection(@NonNull FStream stream)
    {
        if (stream == null)
            throw new IllegalArgumentException("null argument");

        return mMapStreamConnection.get(stream);
    }

    private final class InternalStreamConnection extends StreamConnection
    {
        InternalStreamConnection(@NonNull FStream stream, @NonNull Class<? extends FStream>[] classes)
        {
            super(stream, classes, FStreamManager.this);
        }

        @Override
        protected void onPriorityChanged(int priority, @NonNull FStream stream, @NonNull Class<? extends FStream> clazz)
        {
            final StreamHolder holder = mMapStream.get(clazz);
            if (holder != null)
                holder.onPriorityChanged(priority, stream, clazz);
        }
    }

    @NonNull
    private static Class<? extends FStream>[] getStreamClass(@NonNull FStream stream)
    {
        if (stream == null)
            throw new IllegalArgumentException("null argument");

        final Class<?> sourceClass = stream.getClass();
        final Set<Class<? extends FStream>> set = findAllStreamClass(sourceClass);
        if (set.isEmpty())
            throw new RuntimeException("stream class was not found in stream:" + stream);

        return set.toArray(new Class[set.size()]);
    }

    @NonNull
    private static Set<Class<? extends FStream>> findAllStreamClass(@NonNull Class<?> clazz)
    {
        if (clazz == null)
            throw new IllegalArgumentException("null argument");

        if (Proxy.isProxyClass(clazz))
            throw new IllegalArgumentException("proxy class is not supported");

        final Set<Class<? extends FStream>> set = new HashSet<>();

        while (clazz != null)
        {
            if (!FStream.class.isAssignableFrom(clazz))
                break;

            if (clazz.isInterface())
                throw new RuntimeException("clazz must not be an interface");

            for (Class<?> item : clazz.getInterfaces())
            {
                if (FStream.class.isAssignableFrom(item))
                {
                    set.add((Class<? extends FStream>) item);
                }
            }

            clazz = clazz.getSuperclass();
        }

        return set;
    }

    /**
     * 生成代理对象
     *
     * @param builder
     * @return
     */
    @NonNull
    FStream newProxyInstance(@NonNull FStream.ProxyBuilder builder)
    {
        final Class<?> clazz = builder.mClass;
        final InvocationHandler handler = new FStreamManager.ProxyInvocationHandler(this, builder);
        return (FStream) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, handler);
    }

    private static final class ProxyInvocationHandler implements InvocationHandler
    {
        private final FStreamManager mManager;

        private final Class<? extends FStream> mClass;
        private final Object mTag;
        private final FStream.DispatchCallback mDispatchCallback;
        private final FStream.ResultFilter mResultFilter;

        public ProxyInvocationHandler(@NonNull FStreamManager manager, @NonNull FStream.ProxyBuilder builder)
        {
            if (manager == null || builder == null)
                throw new IllegalArgumentException("null argument");

            mManager = manager;

            mClass = builder.mClass;
            mTag = builder.mTag;
            mDispatchCallback = builder.mDispatchCallback;
            mResultFilter = builder.mResultFilter;
        }

        private boolean checkTag(@NonNull FStream stream)
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
                    Log.i(FStream.class.getSimpleName(), "return type:" + returnType + " but method result is null, so set to " + result
                            + " class:" + mClass.getName());
            }

            if (mManager.isDebug())
                Log.i(FStream.class.getSimpleName(), "notify finish return:" + result + " class:" + mClass.getName());

            return result;
        }

        @Nullable
        private Object processMainLogic(final boolean isVoid, @NonNull final Method method, @Nullable final Object[] args) throws Throwable
        {
            final StreamHolder holder = mManager.mMapStream.get(mClass);
            final int holderSize = holder == null ? 0 : holder.size();

            Collection<FStream> listStream = null;

            if (mManager.isDebug())
            {
                Log.i(FStream.class.getSimpleName(), "notify -----> " + method
                        + " arg:" + (args == null ? "" : Arrays.toString(args))
                        + " tag:" + mTag
                        + " count:" + holderSize);
            }

            boolean isDefaultStream = false;
            if (holderSize <= 0)
            {
                final FStream stream = mManager.getDefaultStream(mClass);
                if (stream == null)
                    return null;

                listStream = new ArrayList<>(1);
                listStream.add(stream);

                isDefaultStream = true;

                if (mManager.isDebug())
                    Log.i(FStream.class.getSimpleName(), "create default stream:" + stream + " for class:" + mClass.getName());
            } else
            {
                listStream = holder.toCollection();
            }

            final boolean filterResult = mResultFilter != null && !isVoid;
            final List<Object> listResult = filterResult ? new LinkedList<>() : null;

            Object result = null;
            int index = 0;
            for (FStream item : listStream)
            {
                final StreamConnection connection = mManager.getConnection(item);
                if (isDefaultStream)
                {
                    // 不判断
                } else
                {
                    if (connection == null)
                        continue;
                }

                if (!checkTag(item))
                    continue;

                if (mDispatchCallback != null)
                {
                    if (mDispatchCallback.beforeDispatch(item, method, args))
                    {
                        if (mManager.isDebug())
                            Log.i(FStream.class.getSimpleName(), "proxy broken dispatch before class:" + mClass.getName());
                        break;
                    }
                }

                Object itemResult = null;
                boolean shouldBreakDispatch = false;

                if (isDefaultStream)
                {
                    itemResult = method.invoke(item, args);
                } else
                {
                    synchronized (mClass)
                    {
                        connection.resetBreakDispatch(mClass);

                        itemResult = method.invoke(item, args);

                        shouldBreakDispatch = connection.shouldBreakDispatch(mClass);
                        connection.resetBreakDispatch(mClass);
                    }
                }

                if (mManager.isDebug())
                {
                    Log.i(FStream.class.getSimpleName(), "notify"
                            + " index:" + index
                            + " return:" + (isVoid ? "" : itemResult)
                            + " stream:" + item
                            + " shouldBreakDispatch:" + shouldBreakDispatch);
                }

                result = itemResult;

                if (filterResult)
                    listResult.add(itemResult);

                if (mDispatchCallback != null)
                {
                    if (mDispatchCallback.afterDispatch(item, method, args, itemResult))
                    {
                        if (mManager.isDebug())
                            Log.i(FStream.class.getSimpleName(), "proxy broken dispatch after class:" + mClass.getName());
                        break;
                    }
                }

                if (shouldBreakDispatch)
                    break;

                index++;
            }

            if (filterResult && !listResult.isEmpty())
            {
                result = mResultFilter.filter(method, args, listResult);

                if (mManager.isDebug())
                    Log.i(FStream.class.getSimpleName(), "proxy filter result: " + result + " class:" + mClass.getName());
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
    public synchronized void registerDefaultStream(@NonNull Class<? extends FStream> clazz)
    {
        if (clazz == null)
            throw new IllegalArgumentException("null argument");

        if (clazz == FStream.class)
            throw new IllegalArgumentException("class must not be " + FStream.class);

        final Set<Class<? extends FStream>> set = findAllStreamClass(clazz);
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
    public synchronized void unregisterDefaultStream(@NonNull Class<? extends FStream> clazz)
    {
        if (clazz == null)
            throw new IllegalArgumentException("null argument");

        if (clazz == FStream.class)
            throw new IllegalArgumentException("class must not be " + FStream.class);

        final Set<Class<? extends FStream>> set = findAllStreamClass(clazz);
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
     * @param factory
     */
    public synchronized void setDefaultStreamFactory(@Nullable DefaultStreamFactory factory)
    {
        mDefaultStreamFactory = factory;
    }

    /**
     * 返回默认的流对象
     *
     * @param clazz
     * @return
     */
    @Nullable
    private synchronized FStream getDefaultStream(@NonNull Class<? extends FStream> clazz)
    {
        if (clazz == null)
            throw new IllegalArgumentException("null argument");

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
}
