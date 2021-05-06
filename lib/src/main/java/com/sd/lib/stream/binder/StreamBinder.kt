package com.sd.lib.stream.binder

import androidx.annotation.CallSuper
import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager
import java.lang.ref.WeakReference

internal abstract class StreamBinder<T> {
    private val _stream: WeakReference<FStream>
    private val _target: WeakReference<T>

    /** 返回要绑定的对象 */
    val target: T? get() = _target.get()

    constructor(stream: FStream, target: T) {
        _stream = WeakReference(stream)
        _target = WeakReference(target)
    }

    /**
     * 绑定
     * @return true-成功  false-失败
     */
    abstract fun bind(): Boolean

    /**
     * 注册流对象
     * @return true-成功  false-失败
     */
    protected fun registerStream(): Boolean {
        val stream = _stream.get() ?: return false
        FStreamManager.getInstance().register(stream)
        return true
    }

    /**
     * 取消注册流对象
     */
    protected fun unregisterStream() {
        val stream = _stream.get() ?: return
        FStreamManager.getInstance().unregister(stream)
    }

    /**
     * 取消注册流对象，并解除绑定关系
     */
    @CallSuper
    open fun destroy() {
        unregisterStream()
        _stream.clear()
    }
}