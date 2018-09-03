package com.sd.lib.stream;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 方法返回值过滤接口
 */
public interface MethodResultFilter
{
    /**
     * 筛选方法的返回值
     *
     * @param method  代理对象被触发的方法
     * @param args    被触发方法的参数
     * @param results 所有注册的流对象该方法的返回值集合
     * @return 返回该方法最终的返回值，默认把返回值集合的最后一个当做该方法的返回值
     */
    Object filterResult(Method method, Object[] args, List<Object> results);
}
