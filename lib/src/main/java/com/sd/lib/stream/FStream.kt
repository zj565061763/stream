package com.sd.lib.stream

import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * 流接口
 */
interface FStream {
    /**
     * 返回当前流对象的tag
     *
     * 代理对象方法被触发的时候，会调用流对象的这个方法返回一个tag用于和代理对象的tag比较，tag相等的流对象才会被通知
     *
     * @param clazz 哪个接口的代理对象方法被触发
     */
    fun getTagForStream(clazz: Class<out FStream>): Any?

    class ProxyBuilder {
        var streamClass: Class<out FStream>? = null
            private set

        var tag: Any? = null
            private set

        var dispatchCallback: DispatchCallback? = null
            private set

        var resultFilter: ResultFilter? = null
            private set

        var isSticky = false
            private set

        /**
         * 设置代理对象的tag
         */
        fun setTag(tag: Any?): ProxyBuilder {
            this.tag = tag
            return this
        }

        /**
         * 设置流对象方法分发回调
         */
        fun setDispatchCallback(callback: DispatchCallback?): ProxyBuilder {
            this.dispatchCallback = callback
            return this
        }

        /**
         * 设置返回值过滤对象
         */
        fun setResultFilter(filter: ResultFilter?): ProxyBuilder {
            this.resultFilter = filter
            return this
        }

        /**
         * 设置是否支持粘性触发
         */
        fun setSticky(sticky: Boolean): ProxyBuilder {
            this.isSticky = sticky
            return this
        }

        /**
         * 创建代理对象
         */
        fun <T : FStream> build(clazz: Class<T>): T {
            require(clazz.isInterface) { "clazz must be an interface" }
            require(clazz != FStream::class.java) { "clazz must not be:${FStream::class.java.name}" }

            this.streamClass = clazz

            val handler = ProxyInvocationHandler(this)
            val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf<Class<*>>(clazz), handler)
            return proxy as T
        }
    }

    interface DispatchCallback {
        /**
         * 流对象的方法被通知之前触发
         *
         * @param stream       流对象
         * @param method       方法
         * @param methodParams 方法参数
         * @return true-停止分发，false-继续分发
         */
        fun beforeDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?): Boolean

        /**
         * 流对象的方法被通知之后触发
         *
         * @param stream       流对象
         * @param method       方法
         * @param methodParams 方法参数
         * @param methodResult 流对象方法被调用后的返回值
         * @return true-停止分发，false-继续分发
         */
        fun afterDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?, methodResult: Any?): Boolean
    }

    interface ResultFilter {
        /**
         * 过滤返回值
         *
         * @param method       方法
         * @param methodParams 方法参数
         * @param results      所有流对象的返回值
         * @return 返回选定的返回值
         */
        fun filter(method: Method, methodParams: Array<Any?>?, results: List<Any?>): Any?
    }

    companion object {
        /**
         * 创建代理对象
         */
        @JvmOverloads
        @JvmStatic
        fun <T : FStream> buildProxy(clazz: Class<T>, block: (ProxyBuilder.() -> Unit)? = null): T {
            val builder = ProxyBuilder()
            block?.invoke(builder)
            return builder.build(clazz)
        }
    }
}