package com.sd.lib.stream.factory

import com.sd.lib.stream.FStream

/**
 * 默认流对象工厂
 */
interface DefaultStreamFactory {
    /**
     * 创建流对象
     */
    fun create(param: CreateParam): FStream

    class CreateParam {
        /** 流接口 */
        val classStream: Class<out FStream>

        /** 流接口实现类 */
        val classStreamDefault: Class<out FStream>

        constructor(classStream: Class<out FStream>, classStreamDefault: Class<out FStream>) {
            this.classStream = classStream
            this.classStreamDefault = classStreamDefault
        }
    }
}