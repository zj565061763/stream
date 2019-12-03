package com.sd.lib.stream.factory;

import com.sd.lib.stream.FStream;

/**
 * 默认流接口实现类对象工厂
 * <p>
 * 如果流接口代理对象的方法被触发的时候未找到与之映射的流对象，那么会调用{@link #create(CreateParam)}方法创建一个流对象来调用
 */
public interface DefaultStreamFactory
{
    /**
     * 创建流对象
     *
     * @param param
     * @return
     */
    FStream create(CreateParam param);

    class CreateParam
    {
        /**
         * 流接口
         */
        public final Class<? extends FStream> classStream;
        /**
         * 流接口实现类
         */
        public final Class<? extends FStream> classDefaultStream;

        public CreateParam(Class<? extends FStream> classStream, Class<? extends FStream> classDefaultStream)
        {
            this.classStream = classStream;
            this.classDefaultStream = classDefaultStream;
        }

        @Override
        public String toString()
        {
            final String superInfo = super.toString();
            return superInfo + "\r\n" +
                    " classStream:" + classStream + "\r\n" +
                    " classDefaultStream:" + classDefaultStream;
        }
    }
}
