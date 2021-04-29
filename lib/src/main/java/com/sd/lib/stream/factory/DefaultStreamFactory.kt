package com.sd.lib.stream.factory

import com.sd.lib.stream.FStream

/**
 * 默认流接口对象工厂
 *
 * 如果流接口代理对象的方法被触发的时候未找到与之映射的流对象，那么会调用[create]方法创建一个流对象来调用
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