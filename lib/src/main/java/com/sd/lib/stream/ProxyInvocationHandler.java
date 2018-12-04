package com.sd.lib.stream;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

final class ProxyInvocationHandler implements InvocationHandler
{
    private final FStreamManager mManager;
    private final Class mClass;
    private final Object mTag;
    private final FStream.DispatchCallback mDispatchCallback;

    public ProxyInvocationHandler(FStream.ProxyBuilder builder, FStreamManager manager)
    {
        mManager = manager;
        mClass = builder.mClass;
        mTag = builder.mTag;
        mDispatchCallback = builder.mDispatchCallback;
    }

    private boolean checkTag(FStream stream)
    {
        final Object tag = stream.getTagForClass(mClass);
        if (mTag == tag)
            return true;

        if (mTag != null && tag != null)
            return mTag.equals(tag);
        else
            return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        final String methodName = method.getName();
        final Class returnType = method.getReturnType();

        final Class<?>[] parameterTypes = method.getParameterTypes();
        if ("getTagForClass".equals(methodName)
                && parameterTypes.length == 1 && parameterTypes[0] == Class.class)
        {
            throw new RuntimeException(methodName + " method can not be called on proxy instance");
        }


        final boolean isVoid = returnType == void.class || returnType == Void.class;
        Object result = processMainLogic(isVoid, method, args);


        if (isVoid)
        {
            result = null;
        } else if (returnType.isPrimitive() && result == null)
        {
            if (boolean.class == returnType)
                result = false;
            else
                result = 0;

            if (mManager.isDebug())
                Log.e(FStream.class.getSimpleName(), "return type:" + returnType + " but method result is null, so set to " + result);
        }

        if (mManager.isDebug() && !isVoid)
            Log.i(FStream.class.getSimpleName(), "notify final return:" + result);

        return result;
    }

    private Object processMainLogic(final boolean isVoid, final Method method, final Object[] args) throws Throwable
    {
        final List<FStream> holder = mManager.getRegisterStream(mClass);
        if (holder == null)
            return null;

        Object result = null;

        if (mManager.isDebug())
            Log.i(FStream.class.getSimpleName(), "notify -----> " + method + " " + (args == null ? "" : Arrays.toString(args)) + " tag:" + mTag + " count:" + holder.size());

        int index = 0;
        for (FStream item : holder)
        {
            if (!checkTag(item))
                continue;

            final Object itemResult = method.invoke(item, args);

            if (mManager.isDebug())
                Log.i(FStream.class.getSimpleName(), "notify index:" + index + " stream:" + item + (isVoid ? "" : (" return:" + itemResult)));

            result = itemResult;

            if (mDispatchCallback != null)
            {
                if (mDispatchCallback.onDispatch(method, args, itemResult, item))
                {
                    if (mManager.isDebug())
                        Log.i(FStream.class.getSimpleName(), "notify breaked");
                    break;
                }
            }

            index++;
        }

        return result;
    }
}
