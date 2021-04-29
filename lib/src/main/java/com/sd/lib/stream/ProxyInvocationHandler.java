package com.sd.lib.stream;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class ProxyInvocationHandler implements InvocationHandler {
    private final FStreamManager mManager;

    private final Class<? extends FStream> mClass;
    private final Object mTag;
    private final FStream.DispatchCallback mDispatchCallback;
    private final FStream.ResultFilter mResultFilter;
    private final boolean mIsSticky;

    public ProxyInvocationHandler(@NonNull FStreamManager manager, @NonNull FStream.ProxyBuilder builder) {
        if (manager == null || builder == null) {
            throw new IllegalArgumentException("null argument");
        }

        mManager = manager;

        mClass = builder.mClass;
        mTag = builder.mTag;
        mDispatchCallback = builder.mDispatchCallback;
        mResultFilter = builder.mResultFilter;
        mIsSticky = builder.mIsSticky;

        if (mIsSticky) {
            StickyInvokeManager.getInstance().proxyCreated(mClass);
        }
    }

    private boolean checkTag(@NonNull FStream stream) {
        final Object tag = stream.getTagForStream(mClass);
        if (mTag == tag) {
            return true;
        }

        return mTag != null && mTag.equals(tag);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String methodName = method.getName();
        final Class<?> returnType = method.getReturnType();

        final Class<?>[] parameterTypes = method.getParameterTypes();
        if ("getTagForStream".equals(methodName) && parameterTypes.length == 1 && parameterTypes[0] == Class.class) {
            return mTag;
        }


        final String uuid = mManager.isDebug() ? UUID.randomUUID().toString() : null;
        final boolean isVoid = returnType == void.class || returnType == Void.class;
        Object result = processMainLogic(isVoid, method, args, uuid);


        if (isVoid) {
            result = null;
        } else if (returnType.isPrimitive() && result == null) {
            if (boolean.class == returnType) {
                result = false;
            } else {
                result = 0;
            }

            if (mManager.isDebug()) {
                Log.i(FStream.class.getSimpleName(), "return type:" + returnType + " but method result is null, so set to " + result
                        + " uuid:" + uuid);
            }
        }

        if (mManager.isDebug()) {
            Log.i(FStream.class.getSimpleName(), "notify finish return:" + result + " uuid:" + uuid);
        }

        if (mIsSticky) {
            StickyInvokeManager.getInstance().proxyInvoke(mClass, mTag, method, args);
        }
        return result;
    }

    @Nullable
    private Object processMainLogic(final boolean isVoid, @NonNull final Method method, @Nullable final Object[] args, final @Nullable String uuid) throws Throwable {
        final StreamHolder holder = mManager.getStreamHolder(mClass);
        final int holderSize = holder == null ? 0 : holder.getSize();

        Collection<FStream> listStream = null;

        if (mManager.isDebug()) {
            Log.i(FStream.class.getSimpleName(), "notify -----> " + method
                    + " arg:" + (args == null ? "" : Arrays.toString(args))
                    + " tag:" + mTag
                    + " count:" + holderSize
                    + " uuid:" + uuid);
        }

        boolean isDefaultStream = false;
        if (holderSize <= 0) {
            final FStream stream = mManager.getDefaultStream(mClass);
            if (stream == null) {
                return null;
            }

            listStream = new ArrayList<>(1);
            listStream.add(stream);

            isDefaultStream = true;

            if (mManager.isDebug()) {
                Log.i(FStream.class.getSimpleName(), "create default stream:" + stream + " uuid:" + uuid);
            }
        } else {
            listStream = holder.toCollection();
        }

        final boolean filterResult = mResultFilter != null && !isVoid;
        final List<Object> listResult = filterResult ? new LinkedList<>() : null;

        Object result = null;
        int index = 0;
        for (FStream item : listStream) {
            final StreamConnection connection = mManager.getConnection(item);
            if (isDefaultStream) {
                // 不判断
            } else {
                if (connection == null) {
                    if (mManager.isDebug()) {
                        Log.e(FStream.class.getSimpleName(), StreamConnection.class.getSimpleName() + " is null uuid:" + uuid);
                    }
                    continue;
                }
            }

            if (!checkTag(item)) {
                continue;
            }

            if (mDispatchCallback != null && mDispatchCallback.beforeDispatch(item, method, args)) {
                if (mManager.isDebug()) {
                    Log.i(FStream.class.getSimpleName(), "proxy broken dispatch before uuid:" + uuid);
                }
                break;
            }

            Object itemResult = null;
            boolean shouldBreakDispatch = false;

            if (isDefaultStream) {
                itemResult = method.invoke(item, args);
            } else {
                synchronized (mClass) {
                    connection.resetBreakDispatch(mClass);

                    itemResult = method.invoke(item, args);

                    shouldBreakDispatch = connection.shouldBreakDispatch(mClass);
                    connection.resetBreakDispatch(mClass);
                }
            }

            if (mManager.isDebug()) {
                Log.i(FStream.class.getSimpleName(), "notify"
                        + " index:" + index
                        + " return:" + (isVoid ? "" : itemResult)
                        + " stream:" + item
                        + " shouldBreakDispatch:" + shouldBreakDispatch
                        + " uuid:" + uuid);
            }

            result = itemResult;

            if (filterResult) {
                listResult.add(itemResult);
            }

            if (mDispatchCallback != null && mDispatchCallback.afterDispatch(item, method, args, itemResult)) {
                if (mManager.isDebug()) {
                    Log.i(FStream.class.getSimpleName(), "proxy broken dispatch after uuid:" + uuid);
                }
                break;
            }

            if (shouldBreakDispatch) {
                break;
            }

            index++;
        }

        if (filterResult && !listResult.isEmpty()) {
            result = mResultFilter.filter(method, args, listResult);

            if (mManager.isDebug()) {
                Log.i(FStream.class.getSimpleName(), "proxy filter result: " + result + " uuid:" + uuid);
            }
        }

        return result;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (mIsSticky) {
            StickyInvokeManager.getInstance().proxyDestroyed(mClass);
        }
    }
}
