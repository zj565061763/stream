package com.sd.lib.stream.factory;

import com.sd.lib.stream.FStream;

public interface DefaultStreamFactory
{
    FStream create(CreateParam param);

    class CreateParam
    {
        public final Class<? extends FStream> classStream;
        public final Class<? extends FStream> classDefaultStream;

        public CreateParam(Class<? extends FStream> classStream, Class<? extends FStream> classDefaultStream)
        {
            this.classStream = classStream;
            this.classDefaultStream = classDefaultStream;
        }
    }
}
