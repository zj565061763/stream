package com.sd.lib.stream;

import java.lang.ref.WeakReference;

public abstract class StreamBinder<T>
{
    private final WeakReference<FStream> mStream;
    private final WeakReference<T> mTarget;

    protected StreamBinder(FStream stream, T target)
    {
        if (stream == null)
            throw new IllegalArgumentException("stream is null when create " + getClass().getName());

        if (target == null)
            throw new IllegalArgumentException("target is null when create " + getClass().getName());

        mStream = new WeakReference<>(stream);
        mTarget = new WeakReference<>(target);
    }

    /**
     * 返回要绑定的对象
     *
     * @return
     */
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

        final Class<? extends FStream>[] classes = FStreamManager.getInstance().registerInternal(stream);
        if (classes.length <= 0)
        {
            destroy();
            return false;
        }

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

        FStreamManager.getInstance().unregisterInternal(stream);
    }

    /**
     * 取消注册流对象，并解除绑定关系
     */
    public void destroy()
    {
        unregisterStream();

        mStream.clear();
        mTarget.clear();
    }
}
