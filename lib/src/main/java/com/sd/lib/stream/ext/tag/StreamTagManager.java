package com.sd.lib.stream.ext.tag;

import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StreamTagManager
{
    private static StreamTagManager sDefault;

    public static StreamTagManager getDefault()
    {
        if (sDefault != null)
            return sDefault;

        synchronized (StreamTagManager.class)
        {
            if (sDefault == null)
                sDefault = new StreamTagManager();
            return sDefault;
        }
    }

    /** 空的Tag */
    public static final String STREAM_TAG_EMPTY = UUID.randomUUID().toString();

    private final Map<IStreamTagView, ViewTree> mMapTagViewTree = new ConcurrentHashMap<>();
    private final Map<View, ViewTree> mMapViewTreeCache = new ConcurrentHashMap<>();

    private boolean mIsDebug;

    public StreamTagManager()
    {
        // 构造方法保持public，可以不使用默认对象
    }

    /**
     * 设置是否调试模式
     *
     * @param debug
     */
    public void setDebug(boolean debug)
    {
        mIsDebug = debug;
    }

    /**
     * 查找View对应的流tag
     *
     * @param view
     * @return
     */
    public synchronized String findStreamTag(View view)
    {
        if (!isAttached(view))
            return STREAM_TAG_EMPTY;

        if (mIsDebug)
        {
            Log.i(StreamTagManager.class.getSimpleName(), "findStreamTag"
                    + " view:" + getObjectId(view));
        }

        final ViewTree tree = findViewTree(view);
        if (tree != null)
        {
            if (mIsDebug)
            {
                Log.i(StreamTagManager.class.getSimpleName(), "findStreamTag success"
                        + " viewTree:" + tree
                        + " view:" + getObjectId(view));
            }

            return tree.getStreamTag();
        }

        final List<View> listChild = new LinkedList<>();
        listChild.add(view);

        ViewParent viewParent = view.getParent();
        while (viewParent != null && viewParent instanceof View)
        {
            final View parent = (View) viewParent;
            if (!isAttached(parent))
                return STREAM_TAG_EMPTY;

            final ViewTree viewTree = findViewTree(parent);
            if (viewTree != null)
            {
                viewTree.addViews(listChild);

                if (mIsDebug)
                {
                    Log.i(StreamTagManager.class.getSimpleName(), "findStreamTag success"
                            + " level:" + listChild.size()
                            + " viewTree:" + viewTree
                            + " viewTreeSize:" + mMapTagViewTree.size()
                            + " cacheTreeSize:" + mMapViewTreeCache.size()
                            + " view:" + getObjectId(view));
                }
                return viewTree.getStreamTag();
            } else
            {
                listChild.add(parent);
                viewParent = parent.getParent();
            }
        }

        throw new RuntimeException(IStreamTagView.class.getSimpleName() + " was not found int view tree " + view);
    }

    private ViewTree findViewTree(View view)
    {
        if (view instanceof IStreamTagView)
        {
            final IStreamTagView tagView = (IStreamTagView) view;
            return getOrCreateViewTree(tagView);
        }

        return mMapViewTreeCache.get(view);
    }

    private synchronized ViewTree getOrCreateViewTree(IStreamTagView tagView)
    {
        if (tagView == null)
            throw new IllegalArgumentException("tagView is null");

        ViewTree viewTree = mMapTagViewTree.get(tagView);
        if (viewTree == null)
        {
            viewTree = new ViewTree(tagView);
            mMapTagViewTree.put(tagView, viewTree);
            initStreamTagView(tagView);

            if (mIsDebug)
            {
                Log.i(StreamTagManager.class.getSimpleName(), "create ViewTree"
                        + " tagView:" + getObjectId(tagView)
                        + " viewTree:" + viewTree
                        + " viewTreeSize:" + mMapTagViewTree.size()
                        + " cacheTreeSize:" + mMapViewTreeCache.size());
            }
        }
        return viewTree;
    }

    private synchronized void removeViewTree(IStreamTagView tagView)
    {
        if (tagView == null)
            throw new IllegalArgumentException("tagView is null");

        mMapTagViewTree.remove(tagView);

        if (mIsDebug)
        {
            Log.i(StreamTagManager.class.getSimpleName(), "remove ViewTree"
                    + " tagView:" + getObjectId(tagView)
                    + " viewTreeSize:" + mMapTagViewTree.size()
                    + " cacheTreeSize:" + mMapViewTreeCache.size());
        }
    }

    private void initStreamTagView(final IStreamTagView tagView)
    {
        final View view = (View) tagView;
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener()
        {
            @Override
            public void onViewAttachedToWindow(View v)
            {
            }

            @Override
            public void onViewDetachedFromWindow(View v)
            {
                v.removeOnAttachStateChangeListener(this);
                removeViewTree(tagView);
            }
        });
    }

    private final class ViewTree implements View.OnAttachStateChangeListener
    {
        private final IStreamTagView nTagView;
        private final Map<View, String> nMapView = new ConcurrentHashMap<>();

        public ViewTree(IStreamTagView tagView)
        {
            if (tagView == null)
                throw new IllegalArgumentException("tagView is null");

            this.nTagView = tagView;
        }

        public String getStreamTag()
        {
            final View view = (View) nTagView;
            return isAttached(view) ? nTagView.getStreamTag() : STREAM_TAG_EMPTY;
        }

        public void addViews(List<View> views)
        {
            for (View view : views)
            {
                if (isAttached(view))
                {
                    final String put = nMapView.put(view, "");
                    if (put == null)
                    {
                        mMapViewTreeCache.put(view, this);
                        view.addOnAttachStateChangeListener(this);
                    }
                }
            }
        }

        @Override
        public void onViewAttachedToWindow(View v)
        {
        }

        @Override
        public void onViewDetachedFromWindow(View v)
        {
            v.removeOnAttachStateChangeListener(this);
            synchronized (StreamTagManager.this)
            {
                nMapView.remove(v);
                mMapViewTreeCache.remove(v);
            }
        }
    }

    public interface IStreamTagView
    {
        String getStreamTag();
    }

    private static boolean isAttached(View view)
    {
        if (view == null)
            return false;

        if (Build.VERSION.SDK_INT >= 19)
            return view.isAttachedToWindow();
        else
            return view.getWindowToken() != null;
    }

    private static String getObjectId(Object object)
    {
        final String className = object.getClass().getName();
        final String hashCode = Integer.toHexString(System.identityHashCode(object));
        return className + "@" + hashCode;
    }
}