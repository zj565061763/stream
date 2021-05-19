package com.sd.demo.stream.utils

import com.sd.lib.stream.FStream

interface TestStream : FStream {
    fun getContent(url: String): String
}

class TestDefaultStream : TestStream {
    override fun getContent(url: String): String {
        return "default@${url}"
    }

    override fun getTagForStream(clazz: Class<out FStream>): Any? {
        return null
    }
}

interface TestStickyStream : FStream {
    fun notifyContent(content: String)
}