package com.sd.lib.stream.ext;

public interface StreamValueCallback<T>
{
    void onSuccess(T value);

    void onError(int code, String desc);
}
