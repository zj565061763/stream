package com.sd.lib.stream;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

    /**
     * {@link #register(FStream, Class[])}
     */
    public <T extends FStream> Class[] register(T stream)
    {
        return register(stream, null);
    }

    /**
     * 注册流对象
     *
     * @param stream
     * @param targetClass 要注册的接口，如果为null则当前流对象实现的所有流接口都会被注册
     * @param <T>
     * @return 返回注册的接口
     */
    public synchronized <T extends FStream> Class[] register(T stream, Class... targetClass)
    {
        final Class[] classes = getStreamClass(stream, targetClass);
        for (Class item : classes)
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
                        Log.i(FStreamManager.class.getSimpleName(), "register:" + stream + " class:" + item.getName() + " tag:" + stream.getTag(item) + " count:" + (holder.size()));
                }
            }
        }
        return classes;
    }

    /**
     * {@link #unregister(FStream, Class[])}
     */
    public <T extends FStream> Class[] unregister(T stream)
    {
        return unregister(stream, null);
    }

    /**
     * 取消注册流对象
     *
     * @param stream
     * @param targetClass 要取消注册的接口，如果为null则当前流对象实现的所有流接口都会被取消注册
     * @param <T>
     * @return 返回取消注册的接口
     */
    public synchronized <T extends FStream> Class[] unregister(T stream, Class... targetClass)
    {
        final Class[] classes = getStreamClass(stream, targetClass);
        for (Class item : classes)
        {
            final List<FStream> holder = MAP_STREAM.get(item);
            if (holder == null)
                continue;

            if (holder.remove(stream))
            {
                if (mIsDebug)
                    Log.e(FStreamManager.class.getSimpleName(), "unregister:" + stream + " class:" + item.getName() + " tag:" + stream.getTag(item) + " count:" + (holder.size()));

                if (holder.isEmpty())
                    MAP_STREAM.remove(item);
            }
        }
        return classes;
    }

    private <T extends FStream> Class[] getStreamClass(T stream, Class... targetClass)
    {
        final Class sourceClass = stream.getClass();
        if (Proxy.isProxyClass(sourceClass))
            throw new IllegalArgumentException("proxy instance is not supported");

        final Set<Class> set = findAllStreamClass(sourceClass);
        if (set.isEmpty())
            throw new IllegalArgumentException("interface extends " + FStream.class.getSimpleName() + " is not found in:" + stream);

        if (targetClass != null && targetClass.length > 0)
        {
            for (Class item : targetClass)
            {
                if (!set.contains(item))
                    throw new RuntimeException("targetClass not found:" + item);
            }
            return targetClass;
        } else
        {
            return set.toArray(new Class[set.size()]);
        }
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

    private static final class ProxyInvocationHandler implements InvocationHandler
    {
        private final FStreamManager mManager;
        private final Class mClass;
        private final Object mTag;
        private final MethodResultFilter mMethodResultFilter;
        private final DispatchCallback mDispatchCallback;

        private final LinkedList<Object> mListResult = new LinkedList<>();

        public ProxyInvocationHandler(ProxyBuilder builder, FStreamManager manager)
        {
            mManager = manager;
            mClass = builder.mClass;
            mTag = builder.mTag;
            mMethodResultFilter = builder.mMethodResultFilter;
            mDispatchCallback = builder.mDispatchCallback;
        }

        private boolean checkTag(FStream stream)
        {
            final Object tag = stream.getTag(mClass);
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
            synchronized (mManager)
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
                final List<FStream> holder = mManager.MAP_STREAM.get(mClass);
                if (holder != null)
                {
                    if (mManager.mIsDebug)
                        Log.i(FStreamManager.class.getSimpleName(), "notify -----> " + method + " " + (args == null ? "" : Arrays.toString(args)) + " tag:" + mTag + " count:" + holder.size());

                    int index = 0;
                    for (FStream item : holder)
                    {
                        if (checkTag(item))
                        {
                            final Object itemResult = method.invoke(item, args);

                            if (mManager.mIsDebug)
                                Log.i(FStreamManager.class.getSimpleName(), "notify index:" + index + " stream:" + item + (isVoid ? "" : (" return:" + itemResult)));

                            if (!isVoid)
                                mListResult.add(itemResult);

                            if (mDispatchCallback != null)
                            {
                                if (mDispatchCallback.onDispatch(method, args, itemResult, item))
                                {
                                    if (mManager.mIsDebug)
                                        Log.i(FStreamManager.class.getSimpleName(), "notify breaked");
                                    break;
                                }
                            }

                            index++;
                        }
                    }

                    if (!mListResult.isEmpty() && !isVoid)
                    {
                        if (mMethodResultFilter != null)
                            result = mMethodResultFilter.filterResult(method, args, mListResult);
                        else
                            result = mListResult.peekLast();
                    }

                    mListResult.clear();
                }
                //---------- main logic end ----------

                if (isVoid)
                {
                    result = null;
                } else if (returnType.isPrimitive() && result == null)
                {
                    if (boolean.class == returnType)
                        result = false;
                    else
                        result = 0;

                    if (mManager.mIsDebug)
                        Log.e(FStreamManager.class.getSimpleName(), "return type:" + returnType + " but method result is null, so set to " + result);
                }

                if (mManager.mIsDebug && !isVoid)
                    Log.i(FStreamManager.class.getSimpleName(), "notify final return:" + result);

                return result;
            }
        }
    }

    public static final class ProxyBuilder
    {
        private Class mClass;
        private Object mTag;
        private MethodResultFilter mMethodResultFilter;
        private DispatchCallback mDispatchCallback;

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
         * 设置流对象方法分发回调
         *
         * @param dispatchCallback
         * @return
         */
        public ProxyBuilder dispatchCallback(DispatchCallback dispatchCallback)
        {
            mDispatchCallback = dispatchCallback;
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
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new ProxyInvocationHandler(this, FStreamManager.getInstance()));
        }
    }
}
