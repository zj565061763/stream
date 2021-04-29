package com.sd.lib.stream;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sd.lib.stream.binder.ActivityStreamBinder;
import com.sd.lib.stream.binder.StreamBinder;
import com.sd.lib.stream.binder.ViewStreamBinder;
import com.sd.lib.stream.factory.DefaultStreamFactory;
import com.sd.lib.stream.factory.WeakCacheDefaultStreamFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流管理类
 */
public class FStreamManager {
    private static final FStreamManager INSTANCE = new FStreamManager();

    private FStreamManager() {
    }

    public static FStreamManager getInstance() {
        return INSTANCE;
    }

    private final Map<Class<? extends FStream>, StreamHolder> mMapStream = new ConcurrentHashMap<>();
    private final Map<FStream, StreamBinder> mMapStreamBinder = new WeakHashMap<>();
    private final Map<FStream, InternalStreamConnection> mMapStreamConnection = new ConcurrentHashMap<>();

    private boolean mIsDebug;

    public boolean isDebug() {
        return mIsDebug;
    }

    public void setDebug(boolean debug) {
        mIsDebug = debug;
    }

    /**
     * {@link #bindStream(FStream, Activity)}
     */
    @Deprecated
    public void bindActivity(@NonNull FStream stream, @NonNull Activity target) {
        bindStream(stream, target);
    }

    /**
     * {@link #bindStream(FStream, View)}
     */
    @Deprecated
    public void bindView(@NonNull FStream stream, @NonNull View target) {
        bindStream(stream, target);
    }

    /**
     * {@link ActivityStreamBinder}
     *
     * @param stream
     * @param target
     * @return true-绑定成功或者已绑定；false-绑定失败
     */
    public synchronized boolean bindStream(@NonNull FStream stream, @NonNull Activity target) {
        if (stream == null || target == null) {
            throw new IllegalArgumentException("null argument");
        }

        final StreamBinder oldBinder = mMapStreamBinder.get(stream);
        if (oldBinder != null) {
            if (oldBinder.getTarget() == target) {
                //  已经绑定过了
                return true;
            } else {
                // target发生变化，先取消绑定
                unbindStream(stream);
            }
        }

        final ActivityStreamBinder binder = new ActivityStreamBinder(stream, target);
        if (binder.bind()) {
            mMapStreamBinder.put(stream, binder);
            if (mIsDebug) {
                Log.i(FStream.class.getSimpleName(), "bind activity"
                        + " stream:" + stream
                        + " target:" + target
                        + " size:" + mMapStreamBinder.size());
            }
            return true;
        }
        return false;
    }

    /**
     * {@link ViewStreamBinder}
     *
     * @param stream
     * @param target
     * @return true-绑定成功或者已绑定；false-绑定失败
     */
    public synchronized boolean bindStream(@NonNull FStream stream, @NonNull View target) {
        if (stream == null || target == null) {
            throw new IllegalArgumentException("null argument");
        }

        final StreamBinder oldBinder = mMapStreamBinder.get(stream);
        if (oldBinder != null) {
            if (oldBinder.getTarget() == target) {
                //  已经绑定过了
                return true;
            } else {
                // target发生变化，先取消绑定
                unbindStream(stream);
            }
        }

        final ViewStreamBinder binder = new ViewStreamBinder(stream, target);
        if (binder.bind()) {
            mMapStreamBinder.put(stream, binder);
            if (mIsDebug) {
                Log.i(FStream.class.getSimpleName(), "bind view"
                        + " stream:" + stream
                        + " target:" + target
                        + " size:" + mMapStreamBinder.size());
            }
            return true;
        }
        return false;
    }

    /**
     * 解绑并取消注册
     *
     * @param stream
     * @return
     */
    public synchronized boolean unbindStream(@NonNull FStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("null argument");
        }

        final StreamBinder binder = mMapStreamBinder.remove(stream);
        if (binder == null) {
            return false;
        }

        binder.destroy();
        if (mIsDebug) {
            Log.i(FStream.class.getSimpleName(), "unbind"
                    + " stream:" + stream
                    + " target:" + binder.getTarget()
                    + " size:" + mMapStreamBinder.size());
        }
        return true;
    }

    /**
     * 注册流对象
     *
     * @param stream
     * @return null-注册失败
     */
    @NonNull
    public synchronized StreamConnection register(@NonNull FStream stream) {
        return registerInternal(stream);
    }

    /**
     * 取消注册流对象
     *
     * @param stream
     */
    public synchronized void unregister(@NonNull FStream stream) {
        unregisterInternal(stream);
    }

    @NonNull
    private StreamConnection registerInternal(@NonNull FStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("null argument");
        }

        InternalStreamConnection streamConnection = mMapStreamConnection.get(stream);
        if (streamConnection != null) {
            // 已经注册过了
            return streamConnection;
        }

        final Class<? extends FStream>[] classes = getStreamClass(stream);
        for (Class<? extends FStream> item : classes) {
            StreamHolder holder = mMapStream.get(item);
            if (holder == null) {
                holder = new StreamHolder(item, FStreamManager.this);
                mMapStream.put(item, holder);
            }

            if (holder.add(stream)) {
                if (mIsDebug) {
                    Log.i(FStream.class.getSimpleName(), "+++++ register"
                            + " class:" + item.getName()
                            + " stream:" + stream
                            + " count:" + (holder.getSize()));
                }
            }
        }

        streamConnection = new InternalStreamConnection(stream, classes);
        mMapStreamConnection.put(stream, streamConnection);
        return streamConnection;
    }

    private void unregisterInternal(@NonNull FStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("null argument");
        }

        final StreamConnection streamConnection = mMapStreamConnection.remove(stream);
        if (streamConnection == null) {
            return;
        }

        final Class<? extends FStream>[] classes = getStreamClass(stream);
        for (Class<? extends FStream> item : classes) {
            final StreamHolder holder = mMapStream.get(item);
            if (holder == null) {
                continue;
            }

            if (holder.remove(stream)) {
                if (holder.getSize() <= 0) {
                    mMapStream.remove(item);
                }

                if (mIsDebug) {
                    Log.i(FStream.class.getSimpleName(), "----- unregister"
                            + " class:" + item.getName()
                            + " stream:" + stream
                            + " count:" + (holder.getSize()));
                }
            }
        }
    }

    StreamHolder getStreamHolder(@NonNull Class<? extends FStream> clazz) {
        return mMapStream.get(clazz);
    }

    /**
     * 返回流对象连接对象
     *
     * @param stream
     * @return
     */
    @Nullable
    public StreamConnection getConnection(@NonNull FStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("null argument");
        }
        return mMapStreamConnection.get(stream);
    }

    private final class InternalStreamConnection extends StreamConnection {
        InternalStreamConnection(@NonNull FStream stream, @NonNull Class<? extends FStream>[] classes) {
            super(stream, classes, FStreamManager.this);
        }

        @Override
        protected void onPriorityChanged(int priority, @NonNull FStream stream, @NonNull Class<? extends FStream> clazz) {
            final StreamHolder holder = mMapStream.get(clazz);
            if (holder != null) {
                holder.notifyPriorityChanged(priority, stream, clazz);
            }
        }
    }

    @NonNull
    private static Class<? extends FStream>[] getStreamClass(@NonNull FStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("null argument");
        }

        final Class<?> sourceClass = stream.getClass();
        final Set<Class<? extends FStream>> set = findAllStreamClass(sourceClass);
        if (set.isEmpty()) {
            throw new RuntimeException("stream class was not found in stream:" + stream);
        }

        return set.toArray(new Class[set.size()]);
    }

    @NonNull
    private static Set<Class<? extends FStream>> findAllStreamClass(@NonNull Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("null argument");
        }

        if (Proxy.isProxyClass(clazz)) {
            throw new IllegalArgumentException("proxy class is not supported");
        }

        final Set<Class<? extends FStream>> set = new HashSet<>();
        while (clazz != null) {
            if (!FStream.class.isAssignableFrom(clazz)) {
                break;
            }

            if (clazz.isInterface()) {
                throw new RuntimeException("clazz must not be an interface");
            }

            for (Class<?> item : clazz.getInterfaces()) {
                if (FStream.class.isAssignableFrom(item)) {
                    set.add((Class<? extends FStream>) item);
                }
            }

            clazz = clazz.getSuperclass();
        }
        return set;
    }

    /**
     * 生成代理对象
     *
     * @param builder
     * @return
     */
    @NonNull
    FStream newProxyInstance(@NonNull FStream.ProxyBuilder builder) {
        final Class<?> clazz = builder.mClass;
        final InvocationHandler handler = new ProxyInvocationHandler(this, builder);
        return (FStream) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, handler);
    }

    //---------- default stream start ----------

    private final Map<Class<? extends FStream>, Class<? extends FStream>> mMapDefaultStreamClass = new ConcurrentHashMap<>();
    private DefaultStreamFactory mDefaultStreamFactory;

    /**
     * 注册默认的流接口实现类
     * <p>
     * {@link DefaultStreamFactory}
     *
     * @param clazz
     */
    public synchronized void registerDefaultStream(@NonNull Class<? extends FStream> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("null argument");
        }

        if (clazz == FStream.class) {
            throw new IllegalArgumentException("class must not be " + FStream.class);
        }

        final Set<Class<? extends FStream>> set = findAllStreamClass(clazz);
        if (set.isEmpty()) {
            throw new IllegalArgumentException("stream class was not found in " + clazz);
        }

        for (Class<? extends FStream> item : set) {
            mMapDefaultStreamClass.put(item, clazz);
        }
    }

    /**
     * 取消注册默认的流接口实现类
     *
     * @param clazz
     */
    public synchronized void unregisterDefaultStream(@NonNull Class<? extends FStream> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("null argument");
        }

        if (clazz == FStream.class) {
            throw new IllegalArgumentException("class must not be " + FStream.class);
        }

        final Set<Class<? extends FStream>> set = findAllStreamClass(clazz);
        if (set.isEmpty()) {
            return;
        }

        for (Class<? extends FStream> item : set) {
            mMapDefaultStreamClass.remove(item);
        }
    }

    /**
     * 设置{@link DefaultStreamFactory}
     *
     * @param factory
     */
    public synchronized void setDefaultStreamFactory(@Nullable DefaultStreamFactory factory) {
        mDefaultStreamFactory = factory;
    }

    /**
     * 返回默认的流对象
     *
     * @param clazz
     * @return
     */
    @Nullable
    synchronized FStream getDefaultStream(@NonNull Class<? extends FStream> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("null argument");
        }

        final Class<? extends FStream> defaultClass = mMapDefaultStreamClass.get(clazz);
        if (defaultClass == null) {
            return null;
        }

        if (mDefaultStreamFactory == null) {
            mDefaultStreamFactory = new WeakCacheDefaultStreamFactory();
        }

        final DefaultStreamFactory.CreateParam param = new DefaultStreamFactory.CreateParam(clazz, defaultClass);
        final FStream stream = mDefaultStreamFactory.create(param);
        if (stream == null) {
            throw new RuntimeException(mDefaultStreamFactory + " create null for param:" + param);
        }
        return stream;
    }

    //---------- default stream end ----------
}