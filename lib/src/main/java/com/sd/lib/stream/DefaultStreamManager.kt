package com.sd.lib.stream

import com.sd.lib.stream.factory.DefaultStreamFactory
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam
import com.sd.lib.stream.factory.WeakCacheDefaultStreamFactory
import com.sd.lib.stream.utils.LibUtils
import java.util.concurrent.ConcurrentHashMap

object DefaultStreamManager {
    private val _mapDefaultStreamClass: MutableMap<Class<out FStream>, Class<out FStream>> = ConcurrentHashMap()
    private var _defaultStreamFactory: DefaultStreamFactory? = null

    /**
     * 注册默认的流接口实现类
     */
    @Synchronized
    fun registerDefaultStream(clazz: Class<out FStream>) {
        val set = LibUtils.findAllStreamClass(clazz)
        require(!set.isEmpty()) { "stream class was not found in $clazz" }
        for (item in set) {
            _mapDefaultStreamClass[item] = clazz
        }
    }

    /**
     * 取消注册默认的流接口实现类
     */
    @Synchronized
    fun unregisterDefaultStream(clazz: Class<out FStream?>) {
        val set = LibUtils.findAllStreamClass(clazz)
        for (item in set) {
            _mapDefaultStreamClass.remove(item)
        }
    }

    /**
     * 设置[DefaultStreamFactory]
     */
    @Synchronized
    fun setDefaultStreamFactory(factory: DefaultStreamFactory?) {
        _defaultStreamFactory = factory
    }

    /**
     * 返回默认的流对象
     */
    @Synchronized
    internal fun getDefaultStream(clazz: Class<out FStream>): FStream? {
        val defaultClass = _mapDefaultStreamClass[clazz] ?: return null

        if (_defaultStreamFactory == null) {
            _defaultStreamFactory = WeakCacheDefaultStreamFactory()
        }
        return _defaultStreamFactory!!.create(CreateParam(clazz, defaultClass))
    }
}