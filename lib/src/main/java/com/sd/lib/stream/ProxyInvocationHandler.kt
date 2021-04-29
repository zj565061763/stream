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
    private final FStreamManager _manager;

    private final Class<? extends FStream> _streamClass;
    private final Object _tag;
    private final FStream.DispatchCallback _dispatchCallback;
    private final FStream.ResultFilter _resultFilter;
    private final boolean _isSticky;

    public ProxyInvocationHandler(@NonNull FStreamManager manager, @NonNull FStream.ProxyBuilder builder) {
        if (manager == null || builder == null) {
            throw new IllegalArgumentException("null argument");
        }

        _manager = manager;

        _streamClass = builder.getStreamClass();
        _tag = builder.getTag();
        _dispatchCallback = builder.getDispatchCallback();
        _resultFilter = builder.getResultFilter();
        _isSticky = builder.isSticky();

        if (_isSticky) {
            StickyInvokeManager.INSTANCE.proxyCreated(_streamClass);
        }
    }

    private boolean checkTag(@NonNull FStream stream) {
        final Object tag = stream.getTagForStream(_streamClass);
        if (_tag == tag) {
            return true;
        }

        return _tag != null && _tag.equals(tag);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String methodName = method.getName();
        final Class<?> returnType = method.getReturnType();

        final Class<?>[] parameterTypes = method.getParameterTypes();
        if ("getTagForStream".equals(methodName) && parameterTypes.length == 1 && parameterTypes[0] == Class.class) {
            return _tag;
        }


        final String uuid = _manager.isDebug() ? UUID.randomUUID().toString() : null;
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

            if (_manager.isDebug()) {
                Log.i(FStream.class.getSimpleName(), "return type:" + returnType + " but method result is null, so set to " + result
                        + " uuid:" + uuid);
            }
        }

        if (_manager.isDebug()) {
            Log.i(FStream.class.getSimpleName(), "notify finish return:" + result + " uuid:" + uuid);
        }

        if (_isSticky) {
            StickyInvokeManager.INSTANCE.proxyInvoke(_streamClass, _tag, method, args);
        }
        return result;
    }

    @Nullable
    private Object processMainLogic(final boolean isVoid, @NonNull final Method method, @Nullable final Object[] args, final @Nullable String uuid) throws Throwable {
        final StreamHolder holder = _manager.getStreamHolder(_streamClass);
        final int holderSize = holder == null ? 0 : holder.getSize();

        Collection<FStream> listStream = null;

        if (_manager.isDebug()) {
            Log.i(FStream.class.getSimpleName(), "notify -----> " + method
                    + " arg:" + (args == null ? "" : Arrays.toString(args))
                    + " tag:" + _tag
                    + " count:" + holderSize
                    + " uuid:" + uuid);
        }

        boolean isDefaultStream = false;
        if (holderSize <= 0) {
            final FStream stream = _manager.getDefaultStream(_streamClass);
            if (stream == null) {
                return null;
            }

            listStream = new ArrayList<>(1);
            listStream.add(stream);

            isDefaultStream = true;

            if (_manager.isDebug()) {
                Log.i(FStream.class.getSimpleName(), "create default stream:" + stream + " uuid:" + uuid);
            }
        } else {
            listStream = holder.toCollection();
        }

        final boolean filterResult = _resultFilter != null && !isVoid;
        final List<Object> listResult = filterResult ? new LinkedList<>() : null;

        Object result = null;
        int index = 0;
        for (FStream item : listStream) {
            final StreamConnection connection = _manager.getConnection(item);
            if (isDefaultStream) {
                // 不判断
            } else {
                if (connection == null) {
                    if (_manager.isDebug()) {
                        Log.e(FStream.class.getSimpleName(), StreamConnection.class.getSimpleName() + " is null uuid:" + uuid);
                    }
                    continue;
                }
            }

            if (!checkTag(item)) {
                continue;
            }

            if (_dispatchCallback != null && _dispatchCallback.beforeDispatch(item, method, args)) {
                if (_manager.isDebug()) {
                    Log.i(FStream.class.getSimpleName(), "proxy broken dispatch before uuid:" + uuid);
                }
                break;
            }

            Object itemResult = null;
            boolean shouldBreakDispatch = false;

            if (isDefaultStream) {
                itemResult = method.invoke(item, args);
            } else {
                synchronized (_streamClass) {
                    connection.resetBreakDispatch(_streamClass);

                    itemResult = method.invoke(item, args);

                    shouldBreakDispatch = connection.shouldBreakDispatch(_streamClass);
                    connection.resetBreakDispatch(_streamClass);
                }
            }

            if (_manager.isDebug()) {
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

            if (_dispatchCallback != null && _dispatchCallback.afterDispatch(item, method, args, itemResult)) {
                if (_manager.isDebug()) {
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
            result = _resultFilter.filter(method, args, listResult);

            if (_manager.isDebug()) {
                Log.i(FStream.class.getSimpleName(), "proxy filter result: " + result + " uuid:" + uuid);
            }
        }

        return result;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (_isSticky) {
            StickyInvokeManager.INSTANCE.proxyDestroyed(_streamClass);
        }
    }
}
