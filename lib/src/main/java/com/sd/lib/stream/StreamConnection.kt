package com.sd.lib.stream

import java.util.concurrent.ConcurrentHashMap

abstract class StreamConnection {
    private val _manager: FStreamManager
    private val _stream: FStream
    private val _mapItem = ConcurrentHashMap<Class<out FStream>, ConnectionItem>()

    internal constructor(stream: FStream, classes: Array<Class<out FStream>>, manager: FStreamManager) {
        _stream = stream
        _manager = manager

        for (item in classes) {
            checkClassInterface(item)
            _mapItem[item] = object : ConnectionItem(item) {
                override fun onPriorityChanged(priority: Int, clazz: Class<out FStream>) {
                    this@StreamConnection.onPriorityChanged(priority, _stream, clazz)
                }
            }
        }
    }

    /**
     * 粘性触发方法
     */
    fun stickyInvoke() {
        for (clazz in _mapItem.keys) {
            StickyInvokeManager.stickyInvoke(_stream, clazz)
        }
    }

    /**
     * 返回优先级
     */
    fun getPriority(clazz: Class<out FStream>): Int {
        checkClassInterface(clazz)
        val item = _mapItem[clazz]
        return item?.iPriority ?: 0
    }

    /**
     * 设置优先级
     */
    @JvmOverloads
    fun setPriority(priority: Int, clazz: Class<out FStream>? = null) {
        if (clazz == null) {
            synchronized(_mapItem) {
                for (item in _mapItem.values) {
                    item.setPriority(priority)
                }
            }
        } else {
            checkClassInterface(clazz)
            checkClassAssignable(clazz)
            _mapItem[clazz]?.setPriority(priority)
        }
    }

    /**
     * 停止分发
     */
    fun breakDispatch(clazz: Class<out FStream>) {
        checkClassInterface(clazz)
        checkClassAssignable(clazz)
        _mapItem[clazz]?.breakDispatch()
    }

    internal fun getItem(clazz: Class<out FStream>): ConnectionItem? {
        checkClassInterface(clazz)
        return _mapItem[clazz]
    }

    private fun checkClassAssignable(clazz: Class<out FStream>) {
        require(clazz.isAssignableFrom(_stream.javaClass)) { "class is not assignable from ${_stream.javaClass.name} class:${clazz.name}" }
    }

    private fun checkClassInterface(clazz: Class<out FStream>) {
        require(clazz.isInterface) { "class must be an interface class:${clazz.name}" }
    }

    /**
     * 优先级变化回调
     */
    protected abstract fun onPriorityChanged(priority: Int, stream: FStream, clazz: Class<out FStream>)
}

internal abstract class ConnectionItem {
    private val _iClass: Class<out FStream>

    /** 优先级  */
    @Volatile
    var iPriority = 0
        private set

    /** 是否停止分发  */
    @Volatile
    var iShouldBreakDispatch = false
        private set

    constructor(clazz: Class<out FStream>) {
        _iClass = clazz
    }

    /**
     * 设置优先级
     */
    @Synchronized
    fun setPriority(priority: Int) {
        if (iPriority != priority) {
            iPriority = priority
            onPriorityChanged(priority, _iClass)
        }
    }

    /**
     * 设置停止分发
     */
    @Synchronized
    fun breakDispatch() {
        iShouldBreakDispatch = true
    }

    /**
     * 重置停止分发标志
     */
    fun resetBreakDispatch() {
        iShouldBreakDispatch = false
    }

    /**
     * 优先级变化回调
     */
    protected abstract fun onPriorityChanged(priority: Int, clazz: Class<out FStream>)
}