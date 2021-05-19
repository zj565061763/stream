package com.sd.demo.stream.utils

import com.sd.lib.stream.FStream

interface TestResultStream : FStream {
    fun onResult(content: String)
}

interface TestGetStream : FStream {
    fun getContent(): String
}
