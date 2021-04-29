package com.sd.lib.stream.factory

import android.util.Log
import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.*

/**
 * 用弱引用缓存流对象的工厂
 */
class WeakCacheDefaultStreamFactory : CacheableDefaultStreamFactory() {
    private val _referenceQueue = ReferenceQueue<FStream>()

    private val _mapStream = HashMap<Class<out FStream>, WeakReference<FStream>>()
    private val _mapReference = HashMap<WeakReference<FStream>, Class<out FStream>>()

    private val _isDebug: Boolean
        private get() = FStreamManager.getInstance().isDebug

    override fun getCache(param: CreateParam): FStream? {
        val reference = _mapStream[param.classStream]
        return reference?.get()
    }

    override fun setCache(param: CreateParam, stream: FStream) {
        releaseReference()

        val reference = WeakReference(stream, _referenceQueue)
        val oldReference = _mapStream.put(param.classStream, reference)
        if (oldReference != null) {
            /**
             * 由于被回收的引用不一定会被及时的添加到ReferenceQueue中，
             * 所以这边判断一下旧的引用不为null的话，要移除掉
             */
            _mapReference.remove(oldReference)
            if (_isDebug) {
                Log.i(WeakCacheDefaultStreamFactory::class.java.simpleName,
                        "remove old reference:${oldReference} ${_sizeLog}")
            }
        }

        _mapReference[reference] = param.classStream
        if (_isDebug) {
            Log.i(WeakCacheDefaultStreamFactory::class.java.simpleName,
                    "+++++ setCache for class:${param.classStream.name} stream:${stream} reference:${reference} ${_sizeLog}")
        }
    }

    private fun releaseReference() {
        var count = 0
        while (true) {
            val reference = _referenceQueue.poll() ?: break

            val clazz = _mapReference.remove(reference)
            if (clazz == null) {
                // 如果为null，说明这个引用已经被手动从map中移除
                if (_isDebug) {
                    Log.i(WeakCacheDefaultStreamFactory::class.java.simpleName,
                            "releaseReference ghost reference was found:$reference")
                }
                continue
            }

            val streamReference = _mapStream.remove(clazz)
            if (streamReference === reference) {
                count++
            } else {
                if (_isDebug) {
                    Log.e(WeakCacheDefaultStreamFactory::class.java.simpleName,
                            "releaseReference  class:${clazz.name} reference:${reference} streamReference:${streamReference}")
                }
            }
        }

        if (count > 0) {
            if (_isDebug) {
                Log.i(WeakCacheDefaultStreamFactory::class.java.simpleName,
                        "releaseReference count:${count} ${_sizeLog}")
            }
        }
    }

    private val _sizeLog: String
        private get() = """
            size:${_mapStream.size},${_mapReference.size}
        """.trimIndent()
}