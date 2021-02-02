package com.sd.lib.stream.ext.tag;

import android.os.Build;
import android.view.View;
import android.view.ViewParent;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    private static final String STREAM_TAG_EMPTY = "";
    private final Map<StreamTagView, ViewTree> mMapTagViewTree = new ConcurrentHashMap<>();

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

        if (view instanceof StreamTagView)
        {
            // 直接返回tag，不创建ViewTree
            final StreamTagView tagView = (StreamTagView) view;
            final String tag = tagView.getStreamTag();
            return tag != null ? tag : STREAM_TAG_EMPTY;
        }

        final List<View> listChild = new LinkedList<>();
        listChild.add(view);

        ViewParent viewParent = view.getParent();
        while (viewParent != null && viewParent instanceof View)
        {
            final View parent = (View) viewParent;
            if (!isAttached(parent))
                break;

            final ViewTree viewTree = findViewTree(parent);
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

    private ViewTree findViewTree(View view)
    {
        if (view instanceof StreamTagView)
        {
            final StreamTagView tagView = (StreamTagView) view;
            return getViewTree(tagView);
        }

        for (ViewTree item : mMapTagViewTree.values())
        {
            if (item.hasView(view))
                return item;
        }
        return null;
    }

    private synchronized ViewTree getViewTree(StreamTagView tagView)
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

    private synchronized void removeViewTree(StreamTagView tagView)
    {
        if (tagView == null)
            throw new IllegalArgumentException("tagView is null");

        mMapTagViewTree.remove(tagView);
    }

    private void initStreamTagView(final StreamTagView tagView)
    {
        final View view = (View) tagView;
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener()
        {
            @Override
            public void onViewAttachedToWindow(View v)
            {
                throw new RuntimeException("onViewAttachedToWindow ??? " + v);
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
        private final StreamTagView nTagView;
        private final Map<View, String> nMapView = new ConcurrentHashMap<>();

        public ViewTree(StreamTagView tagView)
        {
            if (tagView == null)
                throw new IllegalArgumentException("tagView is null");

            this.nTagView = tagView;
        }

        public String getStreamTag()
        {
            final View view = (View) nTagView;
            final boolean isAttached = isAttached(view);
            return isAttached ? nTagView.getStreamTag() : null;
        }

        public void addViews(List<View> views)
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

        public boolean hasView(View view)
        {
            return nMapView.containsKey(view);
        }

        @Override
        public void onViewAttachedToWindow(View v)
        {
            throw new RuntimeException("onViewAttachedToWindow ??? " + v);
        }

        @Override
        public void onViewDetachedFromWindow(View v)
        {
            v.removeOnAttachStateChangeListener(this);
            nMapView.remove(v);
        }
    }

    public interface StreamTagView
    {
        String getStreamTag();

        void setStreamTag(String tag);
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