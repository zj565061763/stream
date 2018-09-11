package com.sd.lib.stream;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 流管理类
 */
public class FStreamManager
{
    private static FStreamManager sInstance;

    private final Map<Class, List<FStream>> MAP_STREAM = new HashMap<>();
    private boolean mIsDebug;

    private FStreamManager()
    {
    }

    public static FStreamManager getInstance()
    {
        if (sInstance == null)
        {
            synchronized (FStreamManager.class)
            {
                if (sInstance == null)
                    sInstance = new FStreamManager();
            }
        }
        return sInstance;
    }

    public void setDebug(boolean debug)
    {
        mIsDebug = debug;
    }

    private String getLogTag()
    {
        return FStreamManager.class.getSimpleName();
    }

    /**
     * 代理对象创建者
     *
     * @return
     */
    public ProxyBuilder newProxyBuilder()
    {
        return new ProxyBuilder();
    }

    /**
     * 注册流对象
     *
     * @param stream
     * @param <T>
     */
    public synchronized <T extends FStream> void register(T stream)
    {
        register(stream, null);
    }

    /**
     * 注册流对象
     *
     * @param stream
     * @param targetClass 要注册的接口，如果为null则当前流对象实现的所有流接口都会被注册
     * @param <T>
     */
    public synchronized <T extends FStream> void register(T stream, Class<T> targetClass)
    {
        final Set<Class> set = getStreamClass(stream, targetClass);
        if (set == null)
            return;

        for (Class item : set)
        {
            List<FStream> holder = MAP_STREAM.get(item);
            if (holder == null)
            {
                holder = new CopyOnWriteArrayList<>();
                MAP_STREAM.put(item, holder);
            }

            if (!holder.contains(stream))
            {
                if (holder.add(stream))
                {
                    if (mIsDebug)
                        Log.i(getLogTag(), "register:" + stream + " class:" + item.getName() + " tag:" + stream.getTag() + " count:" + (holder.size()));
                }
            }
        }
    }

    /**
     * 取消注册流对象
     *
     * @param stream
     * @param <T>
     */
    public synchronized <T extends FStream> void unregister(T stream)
    {
        unregister(stream, null);
    }

    /**
     * 取消注册流对象
     *
     * @param stream
     * @param targetClass 要取消注册的接口，如果为null则当前流对象实现的所有流接口都会被取消注册
     * @param <T>
     */
    public synchronized <T extends FStream> void unregister(T stream, Class<T> targetClass)
    {
        final Set<Class> set = getStreamClass(stream, targetClass);
        if (set == null)
            return;

        for (Class item : set)
        {
            final List<FStream> holder = MAP_STREAM.get(item);
            if (holder != null)
            {
                if (holder.remove(stream))
                {
                    if (mIsDebug)
                        Log.e(getLogTag(), "unregister:" + stream + " class:" + item.getName() + " tag:" + stream.getTag() + " count:" + (holder.size()));
                }

                if (holder.isEmpty())
                    MAP_STREAM.remove(item);
            }
        }
    }

    private <T extends FStream> Set<Class> getStreamClass(T stream, Class<T> targetClass)
    {
        if (stream == null)
            return null;

        final Class sourceClass = stream.getClass();
        if (Proxy.isProxyClass(sourceClass) && FStream.class.isAssignableFrom(sourceClass))
            throw new IllegalArgumentException("proxy instance is not supported");

        final Set<Class> set = findAllStreamClass(sourceClass);
        if (set.isEmpty())
            throw new IllegalArgumentException("interface extends " + FStream.class.getSimpleName() + " is not found in:" + stream);

        if (targetClass != null)
        {
            if (set.contains(targetClass))
            {
                set.clear();
                set.add(targetClass);
            } else
            {
                throw new RuntimeException("targetClass not found:" + targetClass);
            }
        }

        return set;
    }

    private Set<Class> findAllStreamClass(Class clazz)
    {
        final Set<Class> set = new HashSet<>();

        while (true)
        {
            if (clazz == null)
                break;
            if (!FStream.class.isAssignableFrom(clazz))
                break;
            if (clazz.isInterface())
                throw new IllegalArgumentException("clazz must not be an interface");

            for (Class item : clazz.getInterfaces())
            {
                if (FStream.class.isAssignableFrom(item) && FStream.class != item)
                    set.add(item);
            }

            clazz = clazz.getSuperclass();
        }

        return set;
    }

    private final class ProxyInvocationHandler implements InvocationHandler
    {
        private final Class mClass;
        private final Object mTag;
        private final MethodResultFilter mMethodResultFilter;
        private final List<Object> mListResult = new ArrayList<>(1);

        public ProxyInvocationHandler(ProxyBuilder builder)
        {
            mClass = builder.mClass;
            mTag = builder.mTag;
            mMethodResultFilter = builder.mMethodResultFilter;
        }

        private boolean checkTag(FStream stream)
        {
            final Object tag = stream.getTag();
            if (mTag == tag)
                return true;

            if (mTag != null && tag != null)
                return mTag.equals(tag);
            else
                return false;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            synchronized (FStreamManager.this)
            {
                final String methodName = method.getName();
                final Class returnType = method.getReturnType();

                if ("getTag".equals(methodName) && method.getParameterTypes().length == 0)
                {
                    throw new RuntimeException(methodName + " method can not be called on proxy instance");
                }

                final boolean isVoid = returnType == void.class || returnType == Void.class;
                Object result = null;

                //---------- main logic start ----------
                final List<FStream> holder = MAP_STREAM.get(mClass);
                if (holder != null)
                {
                    if (mIsDebug)
                        Log.i(getLogTag(), "notify -----> " + method + " " + (args == null ? "" : Arrays.toString(args)) + " tag:" + mTag + " count:" + holder.size());

                    int notifyCount = 0;
                    for (FStream item : holder)
                    {
                        if (checkTag(item))
                        {
                            final Object itemResult = method.invoke(item, args);

                            if (!isVoid)
                                mListResult.add(itemResult);

                            notifyCount++;
                            if (mIsDebug)
                                Log.i(getLogTag(), "notify index:" + notifyCount + " stream:" + item + (isVoid ? "" : (" return:" + itemResult)));
                        }
                    }

                    if (!mListResult.isEmpty() && !isVoid)
                    {
                        if (mMethodResultFilter != null)
                            result = mMethodResultFilter.filterResult(method, args, mListResult);
                        else
                            result = mListResult.get(mListResult.size() - 1);
                    }

                    mListResult.clear();
                }
                //---------- main logic end ----------

                if (isVoid)
                {
                    result = null;
                } else if (returnType.isPrimitive() && result == null)
                {
                    if (mIsDebug)
                        Log.e(getLogTag(), "return type:" + returnType + " but method result is null, so set to 0");

                    result = 0;
                }

                if (mIsDebug && !isVoid)
                    Log.i(getLogTag(), "notify final return:" + result);

                return result;
            }
        }
    }

    public final class ProxyBuilder
    {
        private Class mClass;
        private Object mTag;
        private MethodResultFilter mMethodResultFilter;

        private ProxyBuilder()
        {
        }

        /**
         * 设置代理对象的tag
         *
         * @param tag
         * @return
         */
        public ProxyBuilder tag(Object tag)
        {
            mTag = tag;
            return this;
        }

        /**
         * 设置方法返回值过滤对象，默认使用最后一个注册的流对象的返回值
         *
         * @param methodResultFilter
         * @return
         */
        public ProxyBuilder methodResultFilter(MethodResultFilter methodResultFilter)
        {
            mMethodResultFilter = methodResultFilter;
            return this;
        }

        /**
         * 创建代理对象
         *
         * @param clazz
         * @param <T>
         * @return
         */
        public <T extends FStream> T build(Class<T> clazz)
        {
            if (clazz == null)
                throw new NullPointerException("clazz is null");
            if (!clazz.isInterface())
                throw new IllegalArgumentException("clazz must be an interface");
            if (clazz == FStream.class)
                throw new IllegalArgumentException("clazz must not be:" + FStream.class.getName());

            mClass = clazz;
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new ProxyInvocationHandler(this));
        }
    }
}
