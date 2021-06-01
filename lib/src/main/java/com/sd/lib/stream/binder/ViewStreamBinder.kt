package com.sd.lib.stream.binder

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import com.sd.lib.stream.FStream

/**
 * 监听[View.OnAttachStateChangeListener]自动注册和取消注册流对象
 */
internal class ViewStreamBinder : StreamBinder<View> {
    constructor(stream: FStream, target: View) : super(stream, target)

    override fun bind(): Boolean {
        val target = target ?: return false

        val context = target.context
        if (context is Activity && context.isFinishing) {
            return false
        }

        val listenerTask = Runnable {
            target.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
            target.addOnAttachStateChangeListener(_onAttachStateChangeListener)
        }

        return if (isAttached(target)) {
            registerStream().also {
                if (it) listenerTask.run()
            }
        } else {
            listenerTask.run()
            true
        }
    }

    private val _onAttachStateChangeListener: OnAttachStateChangeListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            registerStream()
        }

        override fun onViewDetachedFromWindow(v: View) {
            unregisterStream()
        }
    }

    override fun destroy() {
        super.destroy()
        target?.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
    }

    companion object {
        @JvmStatic
        private fun isAttached(view: View): Boolean {
            return if (Build.VERSION.SDK_INT >= 19) {
                view.isAttachedToWindow
            } else {
                view.windowToken != null
            }
        }
    }
}