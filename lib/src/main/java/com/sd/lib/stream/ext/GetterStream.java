package com.sd.lib.stream.ext;

import com.sd.lib.stream.FStream;

public interface GetterStream<T> extends FStream
{
    T get();
}
