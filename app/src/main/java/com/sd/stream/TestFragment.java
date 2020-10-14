package com.sd.stream;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.sd.lib.stream.FStream;

import java.lang.reflect.Method;
import java.util.List;

public class TestFragment extends Fragment
{
    /**
     * 创建接口代理对象
     */
    private final FragmentCallback mCallback = new FStream.ProxyBuilder()
            /**
             * 设置代理对象的tag，默认tag为null
             * 注意：只有tag和当前代理对象tag相等的流对象才会被通知到，tag比较相等的规则为 “==” 或者 “equals”
             */
            .setTag(null)
            /**
             * 设置分发回调
             */
            .setDispatchCallback(new FStream.DispatchCallback()
            {
                @Override
                public boolean beforeDispatch(FStream stream, Method method, Object[] methodParams)
                {
                    return false;
                }

                @Override
                public boolean afterDispatch(FStream stream, Method method, Object[] methodParams, Object methodResult)
                {
                    return false;
                }
            })
            /**
             * 设置返回值过滤
             */
            .setResultFilter(new FStream.ResultFilter()
            {
                @Override
                public Object filter(Method method, Object[] methodParams, List<Object> results)
                {
                    return results.get(0);
                }
            })
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
     * 接口继承流接口
     */
    public interface FragmentCallback extends FStream
    {
        String getActivityContent();
    }
}
