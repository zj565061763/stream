package com.sd.lib.stream

import android.util.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 流对象持有者，保存流接口映射的流对象列表
 */
internal class StreamHolder {
    /** 流接口 */
    private val _class: Class<out FStream>

    /** 流对象 */
    private val _streamHolder: MutableSet<FStream> = LinkedHashSet()

    /** 设置了优先级的流对象  */
    private val _priorityStreamHolder: MutableMap<FStream, Int> = ConcurrentHashMap()

    /** 是否需要排序  */
    @Volatile
    private var _isNeedSort = false

    /** 流对象数量 */
    val size get() = _streamHolder.size

    constructor(clazz: Class<out FStream>) {
        _class = clazz
    }

    /**
     * 添加流对象
     */
    @Synchronized
    fun add(stream: FStream): Boolean {
        val result = _streamHolder.add(stream)
        if (result) {
            if (_priorityStreamHolder.isNotEmpty()) {
                // 如果有流对象设置了优先级，则添加新流对象的时候标记为需要重新排序
                _isNeedSort = true
            }
        }
        return result
    }

    /**
     * 移除流对象
     */
    @Synchronized
    fun remove(stream: FStream): Boolean {
        val result = _streamHolder.remove(stream)
        _priorityStreamHolder.remove(stream)
        return result
    }

    /**
     * 返回流集合
     */
    @Synchronized
    fun toCollection(): Array<FStream> {
        return if (_isNeedSort) {
            sort()
        } else {
            _streamHolder.toTypedArray()
        }
    }

    /**
     * 排序
     */
    private fun sort(): Array<FStream> {
        val array = _streamHolder.toTypedArray()
        if (array.size > 1) {
            // 排序
            array.sortWith(InternalStreamComparator())

            // 把排序后的对象保存到容器
            _streamHolder.clear()
            _streamHolder.addAll(array)
        }

        _isNeedSort = false

        if (FStreamManager.isDebug) {
            Log.i(FStream::class.java.simpleName, "sort stream for class:${_class.name}")
        }
        return array
    }

    /**
     * 通知优先级变化
     */
    @Synchronized
    fun notifyPriorityChanged(priority: Int, stream: FStream, clazz: Class<out FStream>) {
        require(clazz == _class) { "expect class:${_class} but class:${clazz}" }
        require(clazz.isAssignableFrom(stream.javaClass)) { "class:${clazz} is not assignable from ${stream.javaClass}" }

        if (priority == 0) {
            _priorityStreamHolder.remove(stream)
        } else {
            _priorityStreamHolder[stream] = priority
        }

        _isNeedSort = true

        if (FStreamManager.isDebug) {
            Log.i(
                FStream::class.java.simpleName,
                "notifyPriorityChanged priority:${priority} clazz:${clazz.name} priorityStreamHolder size:${_priorityStreamHolder.size} stream:${stream}"
            )
        }
    }

    private inner class InternalStreamComparator : Comparator<FStream> {
        override fun compare(o1: FStream, o2: FStream): Int {
            val o1Connection = FStreamManager.getConnection(o1)
            val o2Connection = FStreamManager.getConnection(o2)

            return if (o1Connection != null && o2Connection != null) {
                o2Connection.getPriority(_class) - o1Connection.getPriority(_class)
            } else {
                0
            }
        }
    }
}