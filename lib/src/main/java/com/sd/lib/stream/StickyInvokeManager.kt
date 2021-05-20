package com.sd.lib.stream

import android.util.Log
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object StickyInvokeManager {
    private val TAG = StickyInvokeManager::class.java.simpleName

    /** 代理对象数量  */
    private val _mapProxyCount = ConcurrentHashMap<Class<out FStream>, Int>()

    /** 保存方法调用信息  */
    private val _mapMethodInfo = ConcurrentHashMap<Class<out FStream>, MutableMap<Any?, MethodInfo>>()

    /**
     * 代理对象创建触发
     */
    fun proxyCreated(clazz: Class<out FStream>) {
        synchronized(clazz) {
            val count = _mapProxyCount[clazz]
            if (count == null) {
                _mapProxyCount[clazz] = 1
            } else {
                _mapProxyCount[clazz] = count + 1
            }

            if (_isDebug) {
                Log.i(TAG, "+++++ proxyCreated class:${clazz.name}  count:${_mapProxyCount[clazz]}")
            }
        }
    }

    /**
     * 代理对象销毁触发
     */
    fun proxyDestroyed(clazz: Class<out FStream>) {
        synchronized(clazz) {
            val count = _mapProxyCount[clazz]
                ?: throw RuntimeException("count is null when destroy proxy:" + clazz.name)

            val targetCount = count - 1
            if (targetCount <= 0) {
                _mapProxyCount.remove(clazz)
                _mapMethodInfo.remove(clazz)
            } else {
                _mapProxyCount[clazz] = targetCount
            }

            if (_isDebug) {
                Log.i(TAG, "----- proxyDestroyed class:${clazz.name}  count:${_mapProxyCount[clazz]}")
            }
        }
    }

    /**
     * 代理方法调用
     */
    fun proxyInvoke(clazz: Class<out FStream>, streamTag: Any?, method: Method, args: Array<Any?>?) {
        if (args == null || args.isEmpty()) {
            // 参数为空，不保存
            return
        }

        val returnType = method.returnType
        val isVoid = returnType == Void.TYPE || returnType == Void::class.java
        if (!isVoid) {
            // 方法有返回值，不保存
            return
        }

        synchronized(clazz) {
            if (!_mapProxyCount.containsKey(clazz)) {
                return
            }

            var holder = _mapMethodInfo[clazz]
            if (holder == null) {
                // holder用HashMap保存，允许key为null，key为tag
                holder = HashMap()
                _mapMethodInfo[clazz] = holder
            }

            var methodInfo = holder[streamTag]
            if (methodInfo == null) {
                methodInfo = MethodInfo()
                holder[streamTag] = methodInfo
            }

            methodInfo.save(method, args)

            if (_isDebug) {
                Log.i(TAG, "proxyInvoke class:${clazz.name} tag:${streamTag} method:${method} args:${Arrays.toString(args)}")
            }
        }
    }

    fun stickyInvoke(stream: FStream, clazz: Class<out FStream>): Boolean {
        require(clazz.isAssignableFrom(stream.javaClass)) { "${clazz.name} is not assignable from stream:${stream}" }
        synchronized(clazz) {
            val holder = _mapMethodInfo[clazz]
            if (holder == null || holder.isEmpty()) {
                return false
            }

            val streamTag = stream.getTagForStream(clazz)
            val methodInfo = holder[streamTag] ?: return false

            if (_isDebug) {
                Log.i(TAG, "stickyInvoke class:${clazz.name} stream:${stream} tag:${streamTag}")
            }

            methodInfo.invoke(stream, clazz)
            return true
        }
    }

    private class MethodInfo {
        private val _iMethodInfo = ConcurrentHashMap<Method, Array<Any?>>()

        /**
         * 保存方法调用信息
         */
        fun save(method: Method, args: Array<Any?>) {
            _iMethodInfo[method] = args
        }

        /**
         * 用保存的方法信息触发流对象的方法
         */
        fun invoke(stream: FStream, clazz: Class<out FStream>) {
            for ((method, args) in _iMethodInfo) {
                if (_isDebug) {
                    Log.i(TAG, "invoke class:${clazz.name} method:${method} args:${Arrays.toString(args)} stream:${stream}")
                }
                method.invoke(stream, *args)
            }
        }
    }

    private val _isDebug: Boolean
        private get() = FStreamManager.isDebug
}