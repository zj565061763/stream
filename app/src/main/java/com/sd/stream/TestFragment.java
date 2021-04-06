package com.sd.stream;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.sd.lib.stream.FStream;

public class TestFragment extends Fragment {
    /**
     * 创建接口代理对象
     */
    private final FragmentCallback mCallback = new FStream.ProxyBuilder()
            /**
             * 设置代理对象的tag，默认tag为null
             * 注意：只有tag和当前代理对象tag相等的流对象才会被通知到，tag比较相等的规则为 “==” 或者 “equals”
             */
            .setTag(null)
            .build(FragmentCallback.class);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Button button = new Button(container.getContext());
        button.setAllCaps(false);
        button.setText("click");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取显示内容
                final String content = mCallback.getDisplayContent();
                button.setText(content);

                final FragmentStickyCallback stickyCallback = new FStream.ProxyBuilder()
                        .setSticky(true) // 设置支持粘性触发
                        .build(FragmentStickyCallback.class);
                stickyCallback.onContent(content);
            }
        });

        return button;
    }

    /**
     * 接口继承流接口
     */
    public interface FragmentCallback extends FStream {
        String getDisplayContent();
    }

    public interface FragmentStickyCallback extends FStream {
        void onContent(String content);
    }
}
