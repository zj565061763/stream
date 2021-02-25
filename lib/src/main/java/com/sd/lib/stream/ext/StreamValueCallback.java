package com.sd.lib.stream.ext;

import androidx.annotation.Nullable;

public interface StreamValueCallback<T>
{
    void onSuccess(@Nullable T value);

    void onError(int code, @Nullable String desc);
}
