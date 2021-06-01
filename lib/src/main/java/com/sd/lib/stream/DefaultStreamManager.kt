package com.sd.lib.stream

import com.sd.lib.stream.factory.DefaultStreamFactory
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam
import com.sd.lib.stream.factory.WeakCacheStreamFactory
import com.sd.lib.stream.utils.LibUtils

/**
 * 默认流接口管理
 */
object DefaultStreamManager {
    private val _mapDefaultStreamClass: MutableMap<Class<out FStream>, Class<out FStream>> = HashMap()

    /** 默认流接口对象工厂 */
    var streamFactory: DefaultStreamFactory = WeakCacheStreamFactory()

    /**
     * 注册默认的流接口实现类
     */
    @Synchronized
    fun register(defaultClass: Class<out FStream>) {
        val classes = LibUtils.findStreamClass(defaultClass)
        for (item in classes) {
            _mapDefaultStreamClass[item] = defaultClass
        }
    }

    /**
     * 取消注册默认的流接口实现类
     */
    @Synchronized
    fun unregister(defaultClass: Class<out FStream>) {
        val classes = LibUtils.findStreamClass(defaultClass)
        for (item in classes) {
            _mapDefaultStreamClass.remove(item)
        }
    }

    /**
     * 返回默认的流对象
     */
    @Synchronized
    internal fun getStream(clazz: Class<out FStream>): FStream? {
        val defaultClass = _mapDefaultStreamClass[clazz] ?: return null
        return streamFactory.create(CreateParam(clazz, defaultClass))
    }
}