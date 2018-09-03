package com.sd.lib.stream;

/**
 * 流接口
 */
public interface FStream
{
    /**
     * 返回当前流对象的tag
     *
     * @return
     */
    default Object getTag()
    {
        return null;
    }
}
