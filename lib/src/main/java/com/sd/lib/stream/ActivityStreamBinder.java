package com.sd.lib.stream;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import java.lang.ref.WeakReference;

/**
 * 将流对象和Activity绑定
 * <p>
 * 在{@link Activity#getWindow()}对象的{@link Window#getDecorView()}被移除的时候取消注册流对象
 */
class ActivityStreamBinder extends StreamBinder<Activity>
{
    private final WeakReference<View> mDecorView;

    protected ActivityStreamBinder(FStream stream, Activity target)
    {
        super(stream, target);

        if (target.isFinishing())
            throw new RuntimeException("Bind stream failed because activity isFinishing");

        final Window window = target.getWindow();
        if (window == null)
            throw new RuntimeException("Bind stream failed because activity's window is null");

        final View decorView = window.getDecorView();
        if (decorView == null)
            throw new RuntimeException("Bind stream failed because activity's window DecorView is null");

        mDecorView = new WeakReference<>(decorView);
    }

    @Override
    public final boolean bind()
    {
        final View decorView = mDecorView.get();
        if (decorView == null)
            return false;

        decorView.removeOnAttachStateChangeListener(mOnAttachStateChangeListener);
        decorView.addOnAttachStateChangeListener(mOnAttachStateChangeListener);

        registerStream();
        return true;
    }

    private final View.OnAttachStateChangeListener mOnAttachStateChangeListener = new View.OnAttachStateChangeListener()
    {
        @Override
        public void onViewAttachedToWindow(View v)
        {
        }

        @Override
        public void onViewDetachedFromWindow(View v)
        {
            destroy();
        }
    };

    @Override
    public void destroy()
    {
        super.destroy();
        final View decorView = mDecorView.get();
        if (decorView != null)
            decorView.removeOnAttachStateChangeListener(mOnAttachStateChangeListener);
    }
}
