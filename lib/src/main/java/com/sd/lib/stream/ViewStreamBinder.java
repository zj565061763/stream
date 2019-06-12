package com.sd.lib.stream;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * 流对象会根据{@link View#isAttachedToWindow()}自动注册和取消注册
 * <p>
 * 注意：不要在以下两个地方绑定，否则有可能导致流对象没办法被自动取消注册<br>
 * 1.绑定View对象的{@link View#onDetachedFromWindow()}方法<br>
 * 2.监听绑定View对象的{@link View.OnAttachStateChangeListener#onViewDetachedFromWindow(View)}方法
 */
class ViewStreamBinder
{
    private final WeakReference<FStream> mStream;
    private final WeakReference<View> mView;

    ViewStreamBinder(FStream stream, View view)
    {
        if (stream == null || view == null)
            throw new IllegalArgumentException("stream or view is null when create " + ViewStreamBinder.class.getName());

        final Context context = view.getContext();
        if (context instanceof Activity)
        {
            if (((Activity) context).isFinishing())
                throw new IllegalArgumentException("Bind stream failed because view's host activity is isFinishing");
        }

        mStream = new WeakReference<>(stream);
        mView = new WeakReference<>(view);

        view.addOnAttachStateChangeListener(mOnAttachStateChangeListener);
        registerStream();
    }

    public final View getView()
    {
        return mView.get();
    }

    private final View.OnAttachStateChangeListener mOnAttachStateChangeListener = new View.OnAttachStateChangeListener()
    {
        @Override
        public void onViewAttachedToWindow(View v)
        {
            registerStream();
        }

        @Override
        public void onViewDetachedFromWindow(View v)
        {
            unregisterStream();
        }
    };

    private void registerStream()
    {
        final FStream stream = mStream.get();
        if (stream != null && isAttached(mView.get()))
        {
            final Class<? extends FStream>[] classes = FStreamManager.getInstance().registerInternal(stream);
            if (classes.length <= 0)
                destroy();
        }
    }

    private void unregisterStream()
    {
        final FStream stream = mStream.get();
        if (stream != null)
            FStreamManager.getInstance().unregisterInternal(stream);
    }

    /**
     * 取消注册流对象，并解除绑定关系
     */
    public final void destroy()
    {
        final View view = mView.get();
        if (view != null)
            view.removeOnAttachStateChangeListener(mOnAttachStateChangeListener);

        unregisterStream();
    }

    private static boolean isAttached(View view)
    {
        if (Build.VERSION.SDK_INT >= 19)
            return view.isAttachedToWindow();
        else
            return view.getWindowToken() != null;
    }
}
