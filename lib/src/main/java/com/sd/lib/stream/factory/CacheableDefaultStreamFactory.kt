package com.sd.lib.stream.factory

import com.sd.lib.stream.FStream
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam

abstract class CacheableDefaultStreamFactory : SimpleDefaultStreamFactory() {

    override fun create(param: CreateParam): FStream {
        var stream = getCache(param)
        if (stream != null) {
            return stream
        }

        stream = super.create(param)
        if (stream != null) {
            setCache(param, stream)
        }
        return stream
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