package com.sd.lib.stream.binder

import android.app.Activity
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.Window
import com.sd.lib.stream.FStream
import java.lang.ref.WeakReference

/**
 * 将流对象和Activity绑定，在[Window.getDecorView]对象被移除的时候取消注册流对象
 */
internal class ActivityStreamBinder : StreamBinder<Activity> {
    private val _decorView: WeakReference<View>

    constructor(stream: FStream, target: Activity) : super(stream, target) {
        val window = target.window
            ?: throw RuntimeException("Bind stream failed because activity's window is null")
        val decorView = window.decorView
            ?: throw RuntimeException("Bind stream failed because activity's window DecorView is null")

        _decorView = WeakReference(decorView)
    }

    override fun bind(): Boolean {
        val activity = target
        if (activity == null || activity.isFinishing) {
            return false
        }

        val decorView = _decorView.get() ?: return false

        if (registerStream()) {
            decorView.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
            decorView.addOnAttachStateChangeListener(_onAttachStateChangeListener)
            return true
        }
        return false
    }

    private val _onAttachStateChangeListener: OnAttachStateChangeListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {

        }

        override fun onViewDetachedFromWindow(v: View) {
            destroy()
        }
    }

    override fun destroy() {
        super.destroy()
        _decorView.get()?.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
    }
}