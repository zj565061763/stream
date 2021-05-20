package com.sd.lib.stream

import android.util.Log
import com.sd.lib.stream.FStream.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.*

internal class ProxyInvocationHandler : InvocationHandler {
    private val _streamClass: Class<out FStream>

    private val _tag: Any?
    private val _dispatchCallback: DispatchCallback?
    private val _resultFilter: ResultFilter?
    private val _isSticky: Boolean

    constructor(builder: ProxyBuilder) {
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
        return _tag == tag
    }

    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        val returnType = method.returnType
        val parameterTypes = method.parameterTypes

        if ("getTagForStream" == method.name &&
            parameterTypes.size == 1 &&
            parameterTypes[0] == Class::class.java
        ) {
            return _tag
        }


        val uuid = if (FStreamManager.isDebug) UUID.randomUUID().toString() else null
        val isVoid = returnType == Void.TYPE || returnType == Void::class.java
        var result = processMainLogic(isVoid, method, args, uuid)


        if (isVoid) {
            result = null
        } else if (returnType.isPrimitive && result == null) {
            result = if (Boolean::class.javaPrimitiveType == returnType) {
                false
            } else {
                0
            }

            if (FStreamManager.isDebug) {
                Log.i(
                    FStream::class.java.simpleName,
                    "return type:${returnType} but method result is null, so set to ${result} uuid:${uuid}"
                )
            }
        }

        if (FStreamManager.isDebug) {
            Log.i(FStream::class.java.simpleName, "notify finish return:${result} uuid:${uuid}")
        }

        if (_isSticky) {
            StickyInvokeManager.proxyInvoke(_streamClass, _tag, method, args)
        }
        return result
    }

    private fun processMainLogic(isVoid: Boolean, method: Method, args: Array<Any?>?, uuid: String?): Any? {
        val holder = FStreamManager.getStreamHolder(_streamClass)
        val listStream = holder?.toCollection()

        if (FStreamManager.isDebug) {
            Log.i(
                FStream::class.java.simpleName, "notify -----> $method"
                        + " arg:${(if (args == null) "" else Arrays.toString(args))}"
                        + " tag:${_tag}"
                        + " count:${listStream?.size ?: 0}"
                        + " uuid:${uuid}"
            )
        }

        if (listStream == null || listStream.isEmpty()) {
            // 尝试创建默认流对象
            val defaultStream = DefaultStreamManager.getStream(_streamClass) ?: return null
            val result = if (args != null) {
                method.invoke(defaultStream, *args)
            } else {
                method.invoke(defaultStream)
            }

            if (FStreamManager.isDebug) {
                val returnLog = if (isVoid) "" else result
                Log.i(FStream::class.java.simpleName, "notify default stream:${defaultStream} return:${returnLog} uuid:${uuid}")
            }
            return result
        }

        val filterResult = _resultFilter != null && !isVoid
        val listResult: MutableList<Any?>? = if (filterResult) LinkedList() else null

        var result: Any? = null
        var index = 0
        for (item in listStream) {
            val connection = FStreamManager.getConnection(item)
            if (connection == null) {
                if (FStreamManager.isDebug) {
                    Log.e(FStream::class.java.simpleName, "${StreamConnection::class.java.simpleName} is null uuid:${uuid}")
                }
                continue
            }

            if (!checkTag(item)) {
                continue
            }

            if (_dispatchCallback != null && _dispatchCallback.beforeDispatch(item, method, args)) {
                if (FStreamManager.isDebug) {
                    Log.i(FStream::class.java.simpleName, "proxy broken dispatch before uuid:${uuid}")
                }
                break
            }

            var itemResult: Any?
            var shouldBreakDispatch: Boolean

            val connectionItem = connection.getItem(_streamClass)!!
            synchronized(connectionItem) {
                connectionItem.resetBreakDispatch()

                // 调用流对象方法
                itemResult = if (args != null) {
                    method.invoke(item, *args)
                } else {
                    method.invoke(item)
                }

                shouldBreakDispatch = connectionItem.shouldBreakDispatch
                connectionItem.resetBreakDispatch()
            }

            if (FStreamManager.isDebug) {
                Log.i(
                    FStream::class.java.simpleName, "notify"
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
                if (FStreamManager.isDebug) {
                    Log.i(FStream::class.java.simpleName, "proxy broken dispatch after uuid:${uuid}")
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
            if (FStreamManager.isDebug) {
                Log.i(FStream::class.java.simpleName, "proxy filter result:${result} uuid:${uuid}")
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