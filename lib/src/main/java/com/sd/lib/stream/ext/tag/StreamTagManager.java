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

    private final Map<StreamTagHolder, ViewTree> mTagViewHolder = new ConcurrentHashMap<>();

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

        final StreamTagHolder streamTagHolder = findStreamTagHolder(view);
        if (streamTagHolder != null)
            return streamTagHolder.getStreamTag();

        final List<View> listView = new LinkedList<>();
        listView.add(view);

        ViewParent viewParent = view.getParent();
        while (viewParent != null && viewParent instanceof View)
        {
            final View parent = (View) viewParent;
            if (!isAttached(parent))
                break;

            final StreamTagHolder tagHolder = findStreamTagHolder(parent);
            if (tagHolder != null)
            {
                final ViewTree viewTree = getViewTree(tagHolder);
                viewTree.addViews(listView);
                return tagHolder.getStreamTag();
            } else
            {
                listView.add(parent);
                viewParent = parent.getParent();
            }
        }

        return null;
    }

    private StreamTagHolder findStreamTagHolder(View view)
    {
        if (view instanceof StreamTagHolder)
        {
            final StreamTagHolder tagHolder = (StreamTagHolder) view;
            getViewTree(tagHolder);
            return tagHolder;
        }

        for (Map.Entry<StreamTagHolder, ViewTree> item : mTagViewHolder.entrySet())
        {
            if (item.getValue().hasView(view))
                return item.getKey();
        }
        return null;
    }

    private synchronized ViewTree getViewTree(StreamTagHolder holder)
    {
        if (holder == null)
            throw new IllegalArgumentException("holder is null");

        ViewTree viewTree = mTagViewHolder.get(holder);
        if (viewTree == null)
        {
            viewTree = new ViewTree(holder);
            mTagViewHolder.put(holder, viewTree);
        }
        return viewTree;
    }

    private synchronized void removeViewTree(StreamTagHolder holder)
    {
        if (holder == null)
            throw new IllegalArgumentException("holder is null");

        mTagViewHolder.remove(holder);
    }

    private final class ViewTree implements View.OnAttachStateChangeListener
    {
        private final StreamTagHolder nTagHolder;
        private final Map<View, String> nViewHolder = new ConcurrentHashMap<>();

        public ViewTree(StreamTagHolder tagHolder)
        {
            this.nTagHolder = tagHolder;
        }

        public void addViews(List<View> views)
        {
            for (View view : views)
            {
                if (isAttached(view))
                {
                    final String put = nViewHolder.put(view, "");
                    if (put == null)
                        view.addOnAttachStateChangeListener(this);
                }
            }
        }

        public boolean hasView(View view)
        {
            return nViewHolder.containsKey(view);
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
            nViewHolder.remove(v);
            if (nViewHolder.isEmpty())
                removeViewTree(nTagHolder);
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