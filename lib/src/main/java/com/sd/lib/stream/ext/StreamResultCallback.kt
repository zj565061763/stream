package com.sd.lib.stream.ext

interface StreamResultCallback<T> {
    fun onSuccess(result: T?)

    fun onError(desc: String?)

    interface Cancelable {
        fun cancel()
    }
}