package com.fanwe.lib.stream;

/**
 * 流接口
 * <p>
 * 当流接口代理对象的方法被调用的时候，会触发和代理对象相同tag的流对象的相同方法
 */
public interface FStream
{
    /**
     * 返回当前流对象的tag
     *
     * @return
     */
    Object getTag();
}
