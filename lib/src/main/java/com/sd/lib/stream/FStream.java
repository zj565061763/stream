package com.sd.lib.stream;

/**
 * 流接口
 */
public interface FStream
{
    /**
     * 返回当前流对象的tag
     * <p>
     * 代理对象方法被触发的时候，会调用流对象的这个方法返回一个tag用于和代理对象的tag比较，tag相等的流对象才会被通知
     *
     * @param clazz 对应哪个接口的方法被触发
     * @return
     */
    default Object getTagForClass(Class<?> clazz)
    {
        return null;
    }
}
