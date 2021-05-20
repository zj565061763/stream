package com.sd.lib.stream.utils

import com.sd.lib.stream.FStream
import java.lang.reflect.Proxy
import java.util.*

internal object LibUtils {
    /**
     * 查找[clazz]的所有流接口
     */
    @JvmStatic
    fun findStreamClass(clazz: Class<*>): Set<Class<out FStream>> {
        require(!Proxy.isProxyClass(clazz)) { "proxy class is not supported" }

        val set = HashSet<Class<out FStream>>()
        var current = clazz
        while (current != null) {
            if (!FStream::class.java.isAssignableFrom(current)) {
                break
            }

            if (current.isInterface) {
                throw RuntimeException("class must not be an interface")
            }

            for (item in current.interfaces) {
                if (FStream::class.java.isAssignableFrom(item)) {
                    set.add(item as Class<out FStream>)
                }
            }

            current = current.superclass
        }
        return set
    }
}