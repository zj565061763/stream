package com.sd.demo.stream.utils

import com.sd.lib.stream.FStream

interface TestStream : FStream {
    fun getContent(url: String): String
}