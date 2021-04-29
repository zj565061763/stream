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
import com.sd.lib.stream.utils.LibUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
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

    private final Map<Class<? extends FStream>, StreamHolder> _mapStream = new ConcurrentHashMap<>();
    private final Map<FStream, StreamBinder> _mapStreamBinder = new WeakHashMap<>();
    private final Map<FStream, InternalStreamConnection> _mapStreamConnection = new ConcurrentHashMap<>();

    private boolean _isDebug;

    public boolean isDebug() {
        return _isDebug;
    }

    public void setDebug(boolean debug) {
        _isDebug = debug;
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

        final StreamBinder oldBinder = _mapStreamBinder.get(stream);
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
            _mapStreamBinder.put(stream, binder);
            if (_isDebug) {
                Log.i(FStream.class.getSimpleName(), "bind activity"
                        + " stream:" + stream
                        + " target:" + target
                        + " size:" + _mapStreamBinder.size());
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

        final StreamBinder oldBinder = _mapStreamBinder.get(stream);
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
            _mapStreamBinder.put(stream, binder);
            if (_isDebug) {
                Log.i(FStream.class.getSimpleName(), "bind view"
                        + " stream:" + stream
                        + " target:" + target
                        + " size:" + _mapStreamBinder.size());
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

        final StreamBinder binder = _mapStreamBinder.remove(stream);
        if (binder == null) {
            return false;
        }

        binder.destroy();
        if (_isDebug) {
            Log.i(FStream.class.getSimpleName(), "unbind"
                    + " stream:" + stream
                    + " target:" + binder.getTarget()
                    + " size:" + _mapStreamBinder.size());
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

        InternalStreamConnection streamConnection = _mapStreamConnection.get(stream);
        if (streamConnection != null) {
            // 已经注册过了
            return streamConnection;
        }

        final Class<? extends FStream>[] classes = getStreamClass(stream);
        for (Class<? extends FStream> item : classes) {
            StreamHolder holder = _mapStream.get(item);
            if (holder == null) {
                holder = new StreamHolder(item, FStreamManager.this);
                _mapStream.put(item, holder);
            }

            if (holder.add(stream)) {
                if (_isDebug) {
                    Log.i(FStream.class.getSimpleName(), "+++++ register"
                            + " class:" + item.getName()
                            + " stream:" + stream
                            + " count:" + (holder.getSize()));
                }
            }
        }

        streamConnection = new InternalStreamConnection(stream, classes);
        _mapStreamConnection.put(stream, streamConnection);
        return streamConnection;
    }

    private void unregisterInternal(@NonNull FStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("null argument");
        }

        final StreamConnection streamConnection = _mapStreamConnection.remove(stream);
        if (streamConnection == null) {
            return;
        }

        final Class<? extends FStream>[] classes = getStreamClass(stream);
        for (Class<? extends FStream> item : classes) {
            final StreamHolder holder = _mapStream.get(item);
            if (holder == null) {
                continue;
            }

            if (holder.remove(stream)) {
                if (holder.getSize() <= 0) {
                    _mapStream.remove(item);
                }

                if (_isDebug) {
                    Log.i(FStream.class.getSimpleName(), "----- unregister"
                            + " class:" + item.getName()
                            + " stream:" + stream
                            + " count:" + (holder.getSize()));
                }
            }
        }
    }

    StreamHolder getStreamHolder(@NonNull Class<? extends FStream> clazz) {
        return _mapStream.get(clazz);
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
        return _mapStreamConnection.get(stream);
    }

    private final class InternalStreamConnection extends StreamConnection {
        InternalStreamConnection(@NonNull FStream stream, @NonNull Class<? extends FStream>[] classes) {
            super(stream, classes, FStreamManager.this);
        }

        @Override
        protected void onPriorityChanged(int priority, @NonNull FStream stream, @NonNull Class<? extends FStream> clazz) {
            final StreamHolder holder = _mapStream.get(clazz);
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

        final Set<Class<? extends FStream>> set = LibUtils.findAllStreamClass(stream.getClass());
        if (set.isEmpty()) {
            throw new RuntimeException("stream class was not found in stream:" + stream);
        }

        return set.toArray(new Class[set.size()]);
    }

    /**
     * 生成代理对象
     *
     * @param builder
     * @return
     */
    @NonNull
    FStream newProxyInstance(@NonNull FStream.ProxyBuilder builder) {
        final Class<?> clazz = builder.getStreamClass();
        final InvocationHandler handler = new ProxyInvocationHandler(builder, this);
        return (FStream) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, handler);
    }

    /**
     * {@link DefaultStreamManager#registerDefaultStream(Class)}
     */
    @Deprecated
    public void registerDefaultStream(@NonNull Class<? extends FStream> clazz) {
        DefaultStreamManager.INSTANCE.registerDefaultStream(clazz);
    }

    /**
     * {@link DefaultStreamManager#unregisterDefaultStream(Class)}
     */
    @Deprecated
    public void unregisterDefaultStream(@NonNull Class<? extends FStream> clazz) {
        DefaultStreamManager.INSTANCE.unregisterDefaultStream(clazz);
    }

    /**
     * {@link DefaultStreamManager#setDefaultStreamFactory(DefaultStreamFactory)}
     */
    @Deprecated
    public void setDefaultStreamFactory(@Nullable DefaultStreamFactory factory) {
        DefaultStreamManager.INSTANCE.setDefaultStreamFactory(factory);
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
}