package com.sd.lib.stream.utils

import com.sd.lib.stream.FStream
import java.lang.reflect.Proxy
import java.util.*

internal object LibUtils {
    /**
     * 查找[clazz]的所有流接口
     */
    @JvmStatic
    fun findStreamClass(clazz: Class<*>): Collection<Class<out FStream>> {
        require(!Proxy.isProxyClass(clazz)) { "proxy class is not supported" }
        val collection = HashSet<Class<out FStream>>()

        var current = clazz
        while (FStream::class.java.isAssignableFrom(current)) {
            if (current.isInterface) {
                throw RuntimeException("class must not be an interface")
            }

            for (item in current.interfaces) {
                if (FStream::class.java.isAssignableFrom(item)) {
                    collection.add(item as Class<out FStream>)
                }
            }

            current = current.superclass ?: break
        }

        if (collection.isEmpty()) throw RuntimeException("stream class was not found in ${clazz}")
        return collection
    }
}