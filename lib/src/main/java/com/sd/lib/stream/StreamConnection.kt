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
            _mapItem[item] = ConnectionItem(item)
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
        synchronized(_manager) {
            /**
             * 由于[StreamHolder.sort]方法中锁住[FStreamManager]对象后根据优先级排序，
             * 所以这边更改优先级的时候也要锁住[FStreamManager]对象
             */
            if (clazz == null) {
                for (item in _mapItem.values) {
                    item.setPriority(priority)
                }
            } else {
                checkClassInterface(clazz)
                checkClassAssignable(clazz)

                val item = _mapItem[clazz]
                item?.setPriority(priority)
            }
        }
    }

    /**
     * 停止分发
     */
    fun breakDispatch(clazz: Class<out FStream>) {
        checkClassInterface(clazz)
        checkClassAssignable(clazz)
        synchronized(clazz) {
            val item = _mapItem[clazz]
            item?.breakDispatch()
        }
    }

    /**
     * 是否需要停止分发
     */
    internal fun shouldBreakDispatch(clazz: Class<out FStream>): Boolean {
        checkClassInterface(clazz)
        val item = _mapItem[clazz]
        return item?.iShouldBreakDispatch ?: false
    }

    /**
     * 重置停止分发标志
     */
    internal fun resetBreakDispatch(clazz: Class<out FStream>) {
        checkClassInterface(clazz)
        val item = _mapItem[clazz]
        item?.resetBreakDispatch()
    }

    private fun checkClassAssignable(clazz: Class<out FStream>) {
        require(clazz.isAssignableFrom(_stream.javaClass)) { "class is not assignable from ${_stream.javaClass.name} class:${clazz.name}" }
    }

    private fun checkClassInterface(clazz: Class<out FStream>) {
        require(clazz.isInterface) { "class must be an interface class:${clazz.name}" }
    }

    private inner class ConnectionItem {
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
         *
         * @param priority
         */
        fun setPriority(priority: Int) {
            if (iPriority != priority) {
                iPriority = priority
                onPriorityChanged(priority, _stream, _iClass)
            }
        }

        /**
         * 设置停止分发
         */
        fun breakDispatch() {
            iShouldBreakDispatch = true
        }

        /**
         * 重置停止分发标志
         */
        fun resetBreakDispatch() {
            iShouldBreakDispatch = false
        }
    }

    /**
     * 优先级变化回调
     */
    protected abstract fun onPriorityChanged(priority: Int, stream: FStream, clazz: Class<out FStream>)
}