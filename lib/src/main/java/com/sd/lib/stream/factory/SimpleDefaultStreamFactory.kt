package com.sd.lib.stream.factory

import com.sd.lib.stream.FStream
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam

open class SimpleDefaultStreamFactory : DefaultStreamFactory {

    override fun create(param: CreateParam): FStream {
        try {
            return param.classStreamDefault.newInstance()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}