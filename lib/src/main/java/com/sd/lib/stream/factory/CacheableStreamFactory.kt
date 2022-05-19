package com.sd.lib.stream.factory

import com.sd.lib.stream.FStream
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam

abstract class CacheableStreamFactory : SimpleStreamFactory() {

    final override fun create(param: CreateParam): FStream {
        val cache = getCache(param)
        if (cache != null) {
            return cache
        }

        val stream = createStream(param)
        setCache(param, stream)
        return stream
    }

    /**
     * 创建Stream对象
     */
    protected fun createStream(param: CreateParam): FStream {
        return super.create(param)
    }

    /**
     * 获取缓存
     */
    protected abstract fun getCache(param: CreateParam): FStream?

    /**
     * 设置缓存
     */
    protected abstract fun setCache(param: CreateParam, stream: FStream)
}