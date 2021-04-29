package com.sd.lib.stream.utils

import com.sd.lib.stream.FStream
import java.lang.reflect.Proxy
import java.util.*

internal object LibUtils {
    /**
     * 查找[clazz]的所有流接口
     */
    @JvmStatic
    fun findAllStreamClass(clazz: Class<out FStream>): Set<Class<out FStream>> {
        require(!Proxy.isProxyClass(clazz)) { "proxy class is not supported" }

        val set = HashSet<Class<out FStream>>()
        var tClass: Class<*> = clazz
        while (tClass != null) {
            if (!FStream::class.java.isAssignableFrom(tClass)) {
                break
            }

            if (tClass.isInterface) {
                throw RuntimeException("class must not be an interface")
            }

            for (item in tClass.interfaces) {
                if (FStream::class.java.isAssignableFrom(item)) {
                    set.add(item as Class<out FStream>)
                }
            }

            tClass = tClass.superclass
        }
        return set
    }
}