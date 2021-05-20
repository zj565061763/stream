package com.sd.lib.stream

import android.util.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class StreamHolder {
    private val _manager: FStreamManager

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

    constructor(clazz: Class<out FStream>, manager: FStreamManager) {
        _class = clazz
        _manager = manager
    }

    /**
     * 添加流对象
     */
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
    fun remove(stream: FStream): Boolean {
        val result = _streamHolder.remove(stream)
        _priorityStreamHolder.remove(stream)
        return result
    }

    /**
     * 返回流集合
     */
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
        synchronized(_manager) {
            val array = _streamHolder.toTypedArray()
            if (array.size > 1) {
                // 排序
                array.sortWith(InternalStreamComparator())

                // 把排序后的对象保存到容器
                _streamHolder.clear()
                _streamHolder.addAll(array)
            }

            _isNeedSort = false

            if (_manager.isDebug) {
                Log.i(FStream::class.java.simpleName, "sort stream for class:" + _class.name)
            }
            return array
        }
    }

    /**
     * 通知优先级变化
     */
    fun notifyPriorityChanged(priority: Int, stream: FStream, clazz: Class<out FStream>) {
        require(clazz == _class) { "expect class:${_class} but class:${clazz}" }

        if (priority == 0) {
            _priorityStreamHolder.remove(stream)
        } else {
            _priorityStreamHolder[stream] = priority
        }

        _isNeedSort = true

        if (_manager.isDebug) {
            Log.i(
                FStream::class.java.simpleName,
                "notifyPriorityChanged priority:${priority} clazz:${clazz.name} priorityStreamHolder size:${_priorityStreamHolder.size}  stream:${stream}"
            )
        }
    }

    private inner class InternalStreamComparator : Comparator<FStream> {
        override fun compare(o1: FStream, o2: FStream): Int {
            val o1Connection = _manager.getConnection(o1)
            val o2Connection = _manager.getConnection(o2)

            if (o1Connection != null && o2Connection != null) {
                return o2Connection.getPriority(_class) - o1Connection.getPriority(_class)
            } else {
                return 0
            }
        }
    }
}