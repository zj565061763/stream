package com.sd.lib.stream

import com.sd.lib.stream.factory.DefaultStreamFactory
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam
import com.sd.lib.stream.factory.WeakCacheStreamFactory
import com.sd.lib.stream.utils.LibUtils

/**
 * 默认流class管理
 *
 * 如果代理对象方法触发的时候未找到注册的流对象，则会查找是否存在默认流class，
 * 如果存在默认流class，则用默认流class创建默认流对象来触发，默认流必须包含空构造方法
 */
object DefaultStreamManager {
    /** 映射流接口和默认流class */
    private val _mapDefaultStreamClass: MutableMap<Class<out FStream>, Class<out FStream>> = HashMap()

    /** 默认流对象工厂 */
    var streamFactory: DefaultStreamFactory = WeakCacheStreamFactory()

    /**
     * 注册默认流class
     */
    @Synchronized
    fun register(defaultClass: Class<out FStream>) {
        val classes = LibUtils.findStreamClass(defaultClass)
        for (item in classes) {
            _mapDefaultStreamClass[item] = defaultClass
        }
    }

    /**
     * 取消注册默认流class
     */
    @Synchronized
    fun unregister(defaultClass: Class<out FStream>) {
        val classes = LibUtils.findStreamClass(defaultClass)
        for (item in classes) {
            _mapDefaultStreamClass.remove(item)
        }
    }

    /**
     * 返回默认流对象
     */
    @Synchronized
    internal fun getStream(clazz: Class<out FStream>): FStream? {
        val defaultClass = _mapDefaultStreamClass[clazz] ?: return null
        return streamFactory.create(CreateParam(clazz, defaultClass))
    }
}