package com.sd.lib.stream;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * 流接口
 */
public interface FStream
{
    /**
     * 返回当前流对象的tag
     * <p>
     * 代理对象方法被触发的时候，会调用流对象的这个方法返回一个tag用于和代理对象的tag比较，tag相等的流对象才会被通知
     *
     * @param clazz 哪个接口的代理对象方法被触发
     * @return
     */
    Object getTagForStream(Class<? extends FStream> clazz);

    class ProxyBuilder
    {
        Class<? extends FStream> mClass;
        Object mTag;
        DispatchCallback mDispatchCallback;
        ResultFilter mResultFilter;

        /**
         * 设置代理对象的tag
         *
         * @param tag
         * @return
         */
        public ProxyBuilder setTag(Object tag)
        {
            mTag = tag;
            return this;
        }

        /**
         * 设置流对象方法分发回调
         *
         * @param callback
         * @return
         */
        public ProxyBuilder setDispatchCallback(DispatchCallback callback)
        {
            mDispatchCallback = callback;
            return this;
        }

        /**
         * 设置返回值过滤对象
         *
         * @param filter
         * @return
         */
        public ProxyBuilder setResultFilter(ResultFilter filter)
        {
            mResultFilter = filter;
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
                throw new IllegalArgumentException("clazz is null");
            if (!clazz.isInterface())
                throw new IllegalArgumentException("clazz must be an interface");
            if (clazz == FStream.class)
                throw new IllegalArgumentException("clazz must not be:" + FStream.class.getName());

            mClass = clazz;
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new FStreamManager.ProxyInvocationHandler(FStreamManager.getInstance(), this));
        }
    }

    interface DispatchCallback
    {
        /**
         * 流对象的方法被通知之前触发
         *
         * @param stream       流对象
         * @param method       方法
         * @param methodParams 方法参数
         * @return true-停止分发，false-继续分发
         */
        boolean beforeDispatch(FStream stream, Method method, Object[] methodParams);

        /**
         * 流对象的方法被通知之后触发
         *
         * @param stream       流对象
         * @param method       方法
         * @param methodParams 方法参数
         * @param methodResult 流对象方法被调用后的返回值
         * @return true-停止分发，false-继续分发
         */
        boolean afterDispatch(FStream stream, Method method, Object[] methodParams, Object methodResult);
    }

    interface ResultFilter
    {
        /**
         * 过滤返回值
         *
         * @param method       方法
         * @param methodParams 方法参数
         * @param results      所有流对象的返回值
         * @return
         */
        Object filter(Method method, Object[] methodParams, List<Object> results);
    }
}
