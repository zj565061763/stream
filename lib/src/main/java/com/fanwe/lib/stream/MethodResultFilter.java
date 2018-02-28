package com.fanwe.lib.stream;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by zhengjun on 2018/2/23.
 */
public class MethodResultFilter
{
    public static final MethodResultFilter DEFAULT = new MethodResultFilter();

    /**
     * 筛选方法的返回值
     *
     * @param method  被触发的方法
     * @param args    被触发的方法的参数
     * @param results 所有注册的FStream对象该方法的返回值集合
     * @return 返回该方法最终的返回值，默认把返回值集合的最后一个当做该方法的返回值
     */
    public Object filterResult(Method method, Object[] args, List<Object> results)
    {
        return results.get(results.size() - 1);
    }
}
