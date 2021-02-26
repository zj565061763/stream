package com.sd.lib.stream.binder;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sd.lib.stream.FStream;
import com.sd.lib.stream.FStreamManager;

import java.lang.ref.WeakReference;

public abstract class StreamBinder<T>
{
    private final WeakReference<FStream> mStream;
    private final WeakReference<T> mTarget;

    public StreamBinder(@NonNull FStream stream, @NonNull T target)
    {
        if (stream == null || target == null)
            throw new IllegalArgumentException("null argument");

        mStream = new WeakReference<>(stream);
        mTarget = new WeakReference<>(target);
    }

    /**
     * 返回要绑定的对象
     *
     * @return
     */
    @Nullable
    public final T getTarget()
    {
        return mTarget.get();
    }

    /**
     * 绑定
     *
     * @return
     */
    public abstract boolean bind();

    /**
     * 注册流对象
     *
     * @return
     */
    protected final boolean registerStream()
    {
        final FStream stream = mStream.get();
        if (stream == null)
            return false;

        FStreamManager.getInstance().register(stream);
        return true;
    }

    /**
     * 取消注册流对象
     */
    protected final void unregisterStream()
    {
        final FStream stream = mStream.get();
        if (stream == null)
            return;

        FStreamManager.getInstance().unregister(stream);
    }

    /**
     * 取消注册流对象，并解除绑定关系
     */
    @CallSuper
    public void destroy()
    {
        unregisterStream();
        mStream.clear();
    }
}
