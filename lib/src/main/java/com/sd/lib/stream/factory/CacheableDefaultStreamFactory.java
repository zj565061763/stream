package com.sd.lib.stream.factory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sd.lib.stream.FStream;

public abstract class CacheableDefaultStreamFactory extends SimpleDefaultStreamFactory {
    @NonNull
    @Override
    public FStream create(@NonNull CreateParam param) {
        FStream stream = getCache(param);
        if (stream != null) {
            return stream;
        }

        stream = super.create(param);
        if (stream != null) {
            setCache(param, stream);
        }

        return stream;
    }

    @Nullable
    protected abstract FStream getCache(@NonNull CreateParam param);

    protected abstract void setCache(@NonNull CreateParam param, @NonNull FStream stream);
}
