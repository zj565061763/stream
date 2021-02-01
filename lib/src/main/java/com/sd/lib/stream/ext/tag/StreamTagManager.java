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

    private final Map<StreamTagHolder, ViewTree> mMapHolderViewTree = new ConcurrentHashMap<>();

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
            return null;

        ViewTree viewTree = findViewTree(view);
        if (viewTree != null)
            return viewTree.getStreamTag();

        final List<View> listChild = new LinkedList<>();
        listChild.add(view);

        ViewParent viewParent = view.getParent();
        while (viewParent != null && viewParent instanceof View)
        {
            final View parent = (View) viewParent;
            if (!isAttached(parent))
                break;

            viewTree = findViewTree(parent);
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

        return null;
    }

    private ViewTree findViewTree(View view)
    {
        if (view instanceof StreamTagHolder)
        {
            final StreamTagHolder tagHolder = (StreamTagHolder) view;
            return getViewTree(tagHolder);
        }

        for (ViewTree item : mMapHolderViewTree.values())
        {
            if (item.hasView(view))
                return item;
        }
        return null;
    }

    private synchronized ViewTree getViewTree(StreamTagHolder holder)
    {
        if (holder == null)
            throw new IllegalArgumentException("holder is null");

        ViewTree viewTree = mMapHolderViewTree.get(holder);
        if (viewTree == null)
        {
            viewTree = new ViewTree(holder);
            mMapHolderViewTree.put(holder, viewTree);
        }
        return viewTree;
    }

    private final class ViewTree implements View.OnAttachStateChangeListener
    {
        private final StreamTagHolder nTagHolder;
        private final Map<View, String> nMapView = new ConcurrentHashMap<>();

        public ViewTree(StreamTagHolder tagHolder)
        {
            if (tagHolder == null)
                throw new IllegalArgumentException("tagHolder is null");

            this.nTagHolder = tagHolder;
        }

        public String getStreamTag()
        {
            return nTagHolder.getStreamTag();
        }

        public void addViews(List<View> views)
        {
            for (View view : views)
            {
                if (isAttached(view))
                {
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
            throw new RuntimeException("onViewAttachedToWindow ??? " + v);
        }

        @Override
        public void onViewDetachedFromWindow(View v)
        {
            v.removeOnAttachStateChangeListener(this);
            nMapView.remove(v);

            synchronized (StreamTagManager.this)
            {
                if (nMapView.isEmpty())
                    mMapHolderViewTree.remove(nMapView);
            }
        }
    }

    public interface StreamTagHolder
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