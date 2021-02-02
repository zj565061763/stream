package com.sd.lib.stream.ext.tag;

import android.os.Build;
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

    public StreamTagManager()
    {
        // 构造方法保持public，可以不使用默认对象
    }

    /**
     * 查找View对应的流tag
     *
     * @param view
     * @return
     */
    public String findStreamTag(View view)
    {
        if (!isAttached(view))
            return STREAM_TAG_EMPTY;

        if (view instanceof IStreamTagView)
        {
            // 直接返回tag，不创建ViewTree
            final IStreamTagView tagView = (IStreamTagView) view;
            return tagView.getStreamTag();
        }

        final ViewTree cacheTree = findViewTreeFromCache(view);
        if (cacheTree != null)
            return cacheTree.getStreamTag();

        final List<View> listChild = new LinkedList<>();
        listChild.add(view);

        ViewParent viewParent = view.getParent();
        while (viewParent != null && viewParent instanceof View)
        {
            final View parent = (View) viewParent;
            if (!isAttached(parent))
                break;

            final ViewTree viewTree = getViewTree(parent);
            if (viewTree != null)
            {
                viewTree.addViews(listChild);
                return viewTree.getStreamTag();
            } else
            {
                listChild.add(parent);
                viewParent = parent.getParent();
            }
        }

        return STREAM_TAG_EMPTY;
    }

    private ViewTree getViewTree(View view)
    {
        if (view instanceof IStreamTagView)
        {
            final IStreamTagView tagView = (IStreamTagView) view;
            return getViewTree(tagView);
        }

        return findViewTreeFromCache(view);
    }

    private ViewTree findViewTreeFromCache(View view)
    {
        return mMapViewTreeCache.get(view);
    }

    private synchronized ViewTree getViewTree(IStreamTagView tagView)
    {
        if (tagView == null)
            throw new IllegalArgumentException("tagView is null");

        ViewTree viewTree = mMapTagViewTree.get(tagView);
        if (viewTree == null)
        {
            viewTree = new ViewTree(tagView);
            mMapTagViewTree.put(tagView, viewTree);
            initStreamTagView(tagView);
        }
        return viewTree;
    }

    private synchronized void removeViewTree(IStreamTagView tagView)
    {
        if (tagView == null)
            throw new IllegalArgumentException("tagView is null");

        mMapTagViewTree.remove(tagView);
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
            final boolean isAttached = isAttached(view);
            return isAttached ? nTagView.getStreamTag() : STREAM_TAG_EMPTY;
        }

        public void addViews(List<View> views)
        {
            synchronized (ViewTree.this)
            {
                for (View view : views)
                {
                    if (!isAttached(view))
                        continue;

                    final String put = nMapView.put(view, "");
                    if (put == null)
                        view.addOnAttachStateChangeListener(this);
                }
            }
        }

        public boolean hasView(View view)
        {
            return nMapView.containsKey(view);
        }

        @Override
        public void onViewAttachedToWindow(View v)
        {
        }

        @Override
        public void onViewDetachedFromWindow(View v)
        {
            v.removeOnAttachStateChangeListener(this);
            synchronized (ViewTree.this)
            {
                nMapView.remove(v);
            }

            mMapViewTreeCache.remove(v);
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
}