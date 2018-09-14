package com.sd.lib.stream;

import java.lang.reflect.Method;

public interface DispatchCallback
{
    /**
     * 流对象的方法被通知的时候回调
     *
     * @param method       方法
     * @param methodParams 方法参数
     * @param methodResult 方法返回值
     * @param observer     流对象
     * @return true-停止分发，false-继续分发
     */
    boolean onDispatch(Method method, Object[] methodParams, Object methodResult, Object observer);
}
