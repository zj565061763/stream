package com.sd.lib.stream.ext;

import androidx.annotation.Nullable;

public interface StreamResultCallback<T> {
    void onSuccess(@Nullable T result);

    void onError(@Nullable String desc);

    interface Cancelable {
        void cancel();
    }
}