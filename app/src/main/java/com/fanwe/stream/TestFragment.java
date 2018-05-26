package com.fanwe.stream;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.fanwe.lib.stream.FStream;
import com.fanwe.lib.stream.FStreamManager;

public class TestFragment extends Fragment
{
    /**
     * 回调代理对象
     */
    private final FragmentCallback mCallback = FStreamManager.getInstance().newProxyBuilder()
            /**
             * 设置代理对象的tag，默认tag为null
             * 注意：只有tag和当前代理对象tag相等的流对象才会被通知到，tag比较相等的规则为 “==” 或者 “equals”
             */
            .tag(null)
            /**
             * 设置方法返回值过滤对象，默认为null，会用最后一个注册的流对象的返回值
             */
            .methodResultFilter(null)
            .build(FragmentCallback.class);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final Button button = new Button(container.getContext());
        button.setText("button");
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                /**
                 * 从Activity中获取内容
                 */
                final String activityContent = mCallback.getActivityContent();
                button.setText(activityContent);
            }
        });

        return button;
    }

    /**
     * Fragment中定义一个接口，接口继承流接口
     */
    public interface FragmentCallback extends FStream
    {
        String getActivityContent();
    }
}
