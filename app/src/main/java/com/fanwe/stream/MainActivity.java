package com.fanwe.stream;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.fanwe.lib.stream.FStreamManager;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 注册回调对象
         */
        FStreamManager.getInstance().register(mCallback1);
        FStreamManager.getInstance().register(mCallback2);
    }

    /**
     * 回调对象
     */
    private final TestTextView.Callback mCallback1 = new TestTextView.Callback()
    {
        @Override
        public Object getTag()
        {
            return null;
        }

        @Override
        public int getTextViewContent()
        {
            return 1;
        }
    };

    /**
     * 回调对象
     */
    private final TestTextView.Callback mCallback2 = new TestTextView.Callback()
    {
        @Override
        public Object getTag()
        {
            return null;
        }

        @Override
        public int getTextViewContent()
        {
            return 2;
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        /**
         * 取消注册，在需要资源释放的地方要取消注册，否则会内存泄漏
         */
        FStreamManager.getInstance().unregister(mCallback1);
        FStreamManager.getInstance().unregister(mCallback2);
    }
}
