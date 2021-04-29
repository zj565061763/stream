package com.sd.lib.stream

import android.util.Log
import com.sd.lib.stream.FStream.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.*

internal class ProxyInvocationHandler : InvocationHandler {
    private val _manager: FStreamManager

    private val _streamClass: Class<out FStream>
    private val _tag: Any?
    private val _dispatchCallback: DispatchCallback?
    private val _resultFilter: ResultFilter?
    private val _isSticky: Boolean

    constructor(manager: FStreamManager, builder: ProxyBuilder) {
        _manager = manager

        _streamClass = builder.streamClass!!
        _tag = builder.tag
        _dispatchCallback = builder.dispatchCallback
        _resultFilter = builder.resultFilter
        _isSticky = builder.isSticky

        if (_isSticky) {
            StickyInvokeManager.proxyCreated(_streamClass)
        }
    }

    private fun checkTag(stream: FStream): Boolean {
        val tag = stream.getTagForStream(_streamClass)
        if (_tag === tag) {
            return true
        }
        return _tag != null && _tag == tag
    }

    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        val returnType = method.returnType
        val parameterTypes = method.parameterTypes

        if ("getTagForStream" == method.name &&
                parameterTypes.size == 1 &&
                parameterTypes[0] == Class::class.java) {
            return _tag
        }


        val uuid = if (_manager.isDebug) UUID.randomUUID().toString() else null
        val isVoid = returnType == Void.TYPE || returnType == Void::class.java
        var result = processMainLogic(isVoid, method, args, uuid)


        if (isVoid) {
            result = null
        } else if (returnType.isPrimitive && result == null) {
            if (Boolean::class.javaPrimitiveType == returnType) {
                result = false
            } else {
                result = 0
            }

            if (_manager.isDebug) {
                Log.i(FStream::class.java.simpleName,
                        "return type:${returnType} but method result is null, so set to ${result} uuid:${uuid}")
            }
        }

        if (_manager.isDebug) {
            Log.i(FStream::class.java.simpleName, "notify finish return:$result uuid:$uuid")
        }

        if (_isSticky) {
            StickyInvokeManager.proxyInvoke(_streamClass, _tag, method, args)
        }
        return result
    }

    @Throws(Throwable::class)
    private fun processMainLogic(isVoid: Boolean, method: Method, args: Array<Any?>?, uuid: String?): Any? {
        val holder = _manager.getStreamHolder(_streamClass)
        val holderSize = holder?.size ?: 0

        if (_manager.isDebug) {
            Log.i(FStream::class.java.simpleName, "notify -----> ${method}"
                    + " arg:${(if (args == null) "" else Arrays.toString(args))}"
                    + " tag:${_tag}"
                    + " count:${holderSize}"
                    + " uuid:${uuid}"
            )
        }

        var listStream: Collection<FStream>? = null
        var isDefaultStream = false

        if (holderSize <= 0) {
            val stream = _manager.getDefaultStream(_streamClass) ?: return null

            listStream = ArrayList(1)
            listStream.add(stream)
            isDefaultStream = true

            if (_manager.isDebug) {
                Log.i(FStream::class.java.simpleName, "create default stream:$stream uuid:$uuid")
            }
        } else {
            listStream = holder!!.toCollection()
        }

        val filterResult = _resultFilter != null && !isVoid
        val listResult: MutableList<Any?>? = if (filterResult) LinkedList() else null

        var result: Any? = null
        var index = 0
        for (item in listStream) {
            val connection = _manager.getConnection(item)
            if (isDefaultStream) {
                // 不判断
            } else {
                if (connection == null) {
                    if (_manager.isDebug) {
                        Log.e(FStream::class.java.simpleName, "${StreamConnection::class.java.simpleName} is null uuid:${uuid}")
                    }
                    continue
                }
            }

            if (!checkTag(item)) {
                continue
            }

            if (_dispatchCallback != null && _dispatchCallback.beforeDispatch(item, method, args)) {
                if (_manager.isDebug) {
                    Log.i(FStream::class.java.simpleName, "proxy broken dispatch before uuid:$uuid")
                }
                break
            }

            var itemResult: Any? = null
            var shouldBreakDispatch = false

            if (isDefaultStream) {
                itemResult = if (args != null) {
                    method.invoke(item, *args)
                } else {
                    method.invoke(item)
                }
            } else {
                synchronized(_streamClass) {
                    connection!!.resetBreakDispatch(_streamClass)

                    // 调用流对象方法
                    itemResult = if (args != null) {
                        method.invoke(item, *args)
                    } else {
                        method.invoke(item)
                    }

                    shouldBreakDispatch = connection.shouldBreakDispatch(_streamClass)
                    connection.resetBreakDispatch(_streamClass)
                }
            }

            if (_manager.isDebug) {
                Log.i(FStream::class.java.simpleName, "notify"
                        + " index:${index}"
                        + " return:${if (isVoid) "" else itemResult}"
                        + " stream:$${item}"
                        + " shouldBreakDispatch:${shouldBreakDispatch}"
                        + " uuid:${uuid}"
                )
            }

            result = itemResult
            if (filterResult) {
                listResult!!.add(itemResult)
            }

            if (_dispatchCallback != null && _dispatchCallback.afterDispatch(item, method, args, itemResult)) {
                if (_manager.isDebug) {
                    Log.i(FStream::class.java.simpleName, "proxy broken dispatch after uuid:$uuid")
                }
                break
            }

            if (shouldBreakDispatch) {
                break
            }

            index++
        }

        if (filterResult && listResult!!.isNotEmpty()) {
            result = _resultFilter!!.filter(method, args, listResult)
            if (_manager.isDebug) {
                Log.i(FStream::class.java.simpleName, "proxy filter result: $result uuid:$uuid")
            }
        }
        return result
    }

    protected fun finalize() {
        if (_isSticky) {
            StickyInvokeManager.proxyDestroyed(_streamClass)
        }
    }
}