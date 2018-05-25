/*
 * Copyright (C) 2018 zhengjun, fanwe (http://www.fanwe.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * 流代理对象创建者
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
     */
    public synchronized void register(FStream stream)
    {
        if (stream == null)
            return;

        final List<Class> list = findStreamClass(stream.getClass());
        if (list.isEmpty())
            throw new IllegalArgumentException("interface extends " + FStream.class.getSimpleName() + " is not found:" + stream);

        for (Class item : list)
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
     */
    public synchronized void unregister(FStream stream)
    {
        if (stream == null)
            return;

        final List<Class> list = findStreamClass(stream.getClass());
        if (list.isEmpty())
            throw new IllegalArgumentException("interface extends " + FStream.class.getSimpleName() + " is not found:" + stream);

        for (Class item : list)
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

    private List<Class> findStreamClass(Class clazz)
    {
        final List<Class> list = new ArrayList<>();

        if (clazz != null)
        {
            if (clazz.isInterface())
                throw new IllegalArgumentException("clazz must not be an interface");

            if (FStream.class.isAssignableFrom(clazz))
            {
                final Class[] interfaces = clazz.getInterfaces();
                for (Class item : interfaces)
                {
                    if (FStream.class.isAssignableFrom(item) && FStream.class != item)
                        list.add(item);
                }
                list.addAll(findStreamClass(clazz.getSuperclass()));
            }
        }

        return list;
    }

    private final class ProxyInvocationHandler implements InvocationHandler
    {
        private final Class nClass;
        private final Object nTag;
        private final MethodResultFilter nMethodResultFilter;
        private final List<Object> nListResult = new ArrayList<>();

        public ProxyInvocationHandler(ProxyBuilder builder)
        {
            nClass = builder.clazz;
            nTag = builder.tag;
            nMethodResultFilter = builder.methodResultFilter;
        }

        private boolean checkTag(FStream stream)
        {
            final Object tag = stream.getTag();
            if (nTag == tag)
                return true;

            if (nTag != null && tag != null)
                return nTag.equals(tag);
            else
                return false;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            synchronized (FStreamManager.this)
            {
                final String methodName = method.getName();
                if ("getTag".equals(methodName))
                    throw new RuntimeException(methodName + " method can not be called on proxy instance");

                Object result = null;

                //---------- main logic start ----------
                final List<FStream> holder = MAP_STREAM.get(nClass);
                if (holder != null)
                {
                    if (mIsDebug)
                    {
                        Log.i(getLogTag(), "notify -----> " + method + " " + (args == null ? "" : Arrays.toString(args)) + " tag:" + nTag + " count:" + holder.size());
                    }

                    int notifyCount = 0;
                    for (FStream item : holder)
                    {
                        if (checkTag(item))
                        {
                            final Object tempResult = method.invoke(item, args);
                            nListResult.add(tempResult);
                            notifyCount++;

                            if (mIsDebug)
                                Log.i(getLogTag(), "notify index:" + notifyCount + " stream:" + item + " return:" + tempResult);
                        }
                    }

                    if (!nListResult.isEmpty())
                    {
                        if (nMethodResultFilter != null)
                            result = nMethodResultFilter.filterResult(method, args, nListResult);
                        else
                            result = nListResult.get(nListResult.size() - 1);

                        nListResult.clear();
                    }
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
                        Log.e(getLogTag(), "return type:" + returnTypeName + " but method result is null");

                    result = 0;
                }

                if (mIsDebug)
                    Log.i(getLogTag(), "notify final return:" + result);

                return result;
            }
        }
    }

    public final class ProxyBuilder
    {
        private Class clazz;
        private Object tag;
        private MethodResultFilter methodResultFilter;

        private ProxyBuilder()
        {
        }

        /**
         * 设置流代理对象的tag
         *
         * @param tag
         * @return
         */
        public ProxyBuilder tag(Object tag)
        {
            this.tag = tag;
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
            this.methodResultFilter = methodResultFilter;
            return this;
        }

        /**
         * 创建流代理对象
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

            this.clazz = clazz;

            final T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new ProxyInvocationHandler(this));
            return proxy;
        }
    }
}
