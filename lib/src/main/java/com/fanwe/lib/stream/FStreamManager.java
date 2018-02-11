package com.fanwe.lib.stream;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

    private final Map<Class, FNotifySession> MAP_NOTIFY_SESSION = new HashMap<>();

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

    public <T extends FStream> T newPublisher(Class<T> clazz)
    {
        return newPublisher(clazz, null);
    }

    public <T extends FStream> T newPublisher(Class<T> clazz, String tag)
    {
        if (!clazz.isInterface())
        {
            throw new IllegalArgumentException("clazz must be an interface");
        }
        if (clazz == FStream.class)
        {
            throw new IllegalArgumentException("clazz must not be:" + FStream.class.getName());
        }

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new ProxyInvocationHandler(clazz, tag));
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
            MAP_NOTIFY_SESSION.remove(clazz);
        }
    }

    synchronized FNotifySession getNotifySession(Class clazz)
    {
        if (!MAP_STREAM.containsKey(clazz))
        {
            throw new RuntimeException("can not call getNotifySession() before stream is register");
        }

        FNotifySession session = MAP_NOTIFY_SESSION.get(clazz);
        if (session == null)
        {
            session = new FNotifySession();
            MAP_NOTIFY_SESSION.put(clazz, session);
        }
        return session;
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
        private Class nClass;
        private Object nTag;

        public ProxyInvocationHandler(Class clazz, Object tag)
        {
            nClass = clazz;
            nTag = tag;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            synchronized (FStreamManager.this)
            {
                final String name = method.getName();
                if ("register".equals(name)
                        || "unregister".equals(name)
                        || "getNotifySession".equals(name)
                        || "getTag".equals(name))
                {
                    throw new RuntimeException(name + " method can not be called on proxy instance");
                }
                final List<FStream> holder = MAP_STREAM.get(nClass);
                if (holder == null)
                {
                    return null;
                }

                if (mIsDebug)
                {
                    Log.i(getLogTag(), "notify method -----> " + method + " " + (args == null ? "" : Arrays.toString(args)) + " tag(" + nTag + ")");
                }

                Object result = null;

                final FNotifySession session = getNotifySession(nClass);
                session.reset();

                int notifyCount = 0;
                Object tempResult = null;
                for (Object item : holder)
                {
                    final FStream stream = (FStream) item;
                    if (nTag == stream.getTag())
                    {
                        tempResult = method.invoke(item, args);
                        session.MAP_RESULT.put(stream, tempResult);
                        notifyCount++;

                        if (mIsDebug)
                        {
                            Log.i(getLogTag(), "notify " + notifyCount + " " + stream);
                        }
                    }
                }
                if (mIsDebug)
                {
                    Log.i(getLogTag(), "notifyCount:" + notifyCount + " totalCount:" + holder.size());
                }

                final FStream requestAsResultStream = session.getRequestAsResultStream();
                if (requestAsResultStream != null)
                {
                    result = session.MAP_RESULT.get(requestAsResultStream);
                } else
                {
                    result = tempResult;
                }

                final Class returnType = method.getReturnType();
                final String returnTypeName = returnType.getName();
                if (returnType.isPrimitive() && !"void".equals(returnTypeName) && result == null)
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

                session.reset();
                return result;
            }
        }
    }
}
