package com.fanwe.lib.stream;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by zhengjun on 2018/2/9.
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
                {
                    sInstance = new FStreamManager();
                }
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

    public <T extends FStream> T newProxy(Class<T> clazz)
    {
        return newProxy(clazz, null);
    }

    public <T extends FStream> T newProxy(Class<T> clazz, String tag)
    {
        return newProxy(clazz, tag, null);
    }

    public <T extends FStream> T newProxy(Class<T> clazz, String tag, MethodResultFilter methodResultFilter)
    {
        if (!clazz.isInterface())
        {
            throw new IllegalArgumentException("clazz must be an interface");
        }
        if (clazz == FStream.class)
        {
            throw new IllegalArgumentException("clazz must not be:" + FStream.class.getName());
        }

        T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new ProxyInvocationHandler(clazz, tag, methodResultFilter));
        if (mIsDebug)
        {
            Log.i(getLogTag(), "proxy created:" + clazz.getName() + " tag(" + tag + ")");
        }
        return proxy;
    }

    synchronized void register(FStream stream)
    {
        if (stream == null)
        {
            return;
        }

        final Class clazz = getStreamClass(stream);
        List<FStream> holder = MAP_STREAM.get(clazz);
        if (holder == null)
        {
            holder = new CopyOnWriteArrayList<>();
            MAP_STREAM.put(clazz, holder);
        }

        if (holder.contains(stream))
        {
            return;
        }
        if (holder.add(stream))
        {
            if (mIsDebug)
            {
                Log.i(getLogTag(), "register:" + stream + " (" + clazz.getName() + ") tag(" + stream.getTag() + ") " + (holder.size()));
            }
        }
    }

    synchronized void unregister(FStream stream)
    {
        if (stream == null)
        {
            return;
        }

        final Class clazz = getStreamClass(stream);
        final List<FStream> holder = MAP_STREAM.get(clazz);
        if (holder == null)
        {
            return;
        }

        if (holder.remove(stream))
        {
            if (mIsDebug)
            {
                Log.e(getLogTag(), "unregister:" + stream + " (" + clazz.getName() + ") tag(" + stream.getTag() + ") " + (holder.size()));
            }
        }

        if (holder.isEmpty())
        {
            MAP_STREAM.remove(clazz);
        }
    }

    Class getStreamClass(FStream stream)
    {
        final Class[] arrInterface = stream.getClass().getInterfaces();
        if (arrInterface.length != 1)
        {
            throw new IllegalArgumentException("stream can only implements one interface");
        } else
        {
            return arrInterface[0];
        }
    }

    private final class ProxyInvocationHandler implements InvocationHandler
    {
        private final Class nClass;
        private final String nTag;
        private final MethodResultFilter nMethodResultFilter;
        private final List<Object> nListResult = new ArrayList<>();

        public ProxyInvocationHandler(Class clazz, String tag, MethodResultFilter methodResultFilter)
        {
            nClass = clazz;
            nTag = tag;
            if (methodResultFilter == null)
            {
                methodResultFilter = new MethodResultFilter();
            }
            nMethodResultFilter = methodResultFilter;
        }

        private boolean checkTag(FStream stream)
        {
            final String tag = stream.getTag();
            if (nTag == tag)
            {
                return true;
            }

            if (nTag != null && tag != null)
            {
                return nTag.equals(tag);
            } else
            {
                return false;
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            synchronized (FStreamManager.this)
            {
                final String methodName = method.getName();
                if ("register".equals(methodName)
                        || "unregister".equals(methodName)
                        || "getTag".equals(methodName))
                {
                    throw new RuntimeException(methodName + " method can not be called on proxy instance");
                }

                nListResult.clear();
                Object result = null;

                //---------- main logic start ----------
                final List<FStream> holder = MAP_STREAM.get(nClass);
                if (holder != null)
                {
                    if (mIsDebug)
                    {
                        Log.i(getLogTag(), "notify method -----> " + method + " " + (args == null ? "" : Arrays.toString(args)) + " tag(" + nTag + ")");
                    }

                    int notifyCount = 0;
                    for (FStream item : holder)
                    {
                        if (checkTag(item))
                        {
                            Object tempResult = method.invoke(item, args);
                            nListResult.add(tempResult);
                            notifyCount++;

                            if (mIsDebug)
                            {
                                Log.i(getLogTag(), "notify index:" + notifyCount + " item:" + item + " result:" + tempResult);
                            }
                        }
                    }
                    if (mIsDebug)
                    {
                        Log.i(getLogTag(), "notifyCount:" + notifyCount + " totalCount:" + holder.size());
                    }

                    result = nMethodResultFilter.filterResult(method, args, nListResult);
                }
                //---------- main logic end ----------

                final Class returnType = method.getReturnType();
                final String returnTypeName = returnType.getName();
                if ("void".equals(returnTypeName))
                {
                    result = null;
                } else if (returnType.isPrimitive() && result == null)
                {
                    if (mIsDebug)
                    {
                        Log.e(getLogTag(), "return type:" + returnTypeName + " but result:" + result);
                    }
                    result = 0;
                }

                if (mIsDebug)
                {
                    Log.i(getLogTag(), "notify result " + result);
                }

                nListResult.clear();
                return result;
            }
        }
    }
}
