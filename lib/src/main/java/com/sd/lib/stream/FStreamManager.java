package com.sd.lib.stream;

import android.util.Log;

import java.lang.reflect.Proxy;
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

    public boolean isDebug()
    {
        return mIsDebug;
    }

    public void setDebug(boolean debug)
    {
        mIsDebug = debug;
    }

    /**
     * 返回已注册的对象
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T extends FStream> List<FStream> getRegisteredStream(Class<T> clazz)
    {
        return MAP_STREAM.get(clazz);
    }

    /**
     * {@link #register(FStream, Class[])}
     */
    public <T extends FStream> Class[] register(T stream)
    {
        return register(stream, (Class[]) null);
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
                        Log.i(FStream.class.getSimpleName(), "register:" + stream + " class:" + item.getName() + " tag:" + stream.getTagForClass(item) + " count:" + (holder.size()));
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
        return unregister(stream, (Class[]) null);
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
                    Log.e(FStream.class.getSimpleName(), "unregister:" + stream + " class:" + item.getName() + " tag:" + stream.getTagForClass(item) + " count:" + (holder.size()));

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
        {
            // 改为日志输出，不抛异常
            if (mIsDebug)
                Log.e(FStream.class.getSimpleName(), "interface extends " + FStream.class.getSimpleName() + " is not found in:" + stream);
        }

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
}
