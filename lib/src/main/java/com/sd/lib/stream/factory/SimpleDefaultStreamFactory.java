package com.sd.lib.stream.factory;

import androidx.annotation.NonNull;

import com.sd.lib.stream.FStream;

public class SimpleDefaultStreamFactory implements DefaultStreamFactory {
    @NonNull
    @Override
    public FStream create(@NonNull CreateParam param) {
        try {
            return param.classDefaultStream.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
