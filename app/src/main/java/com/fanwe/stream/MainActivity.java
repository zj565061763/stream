package com.fanwe.stream;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCallback1.register(); // 注册回调对象
        mCallback2.register();
    }

    /**
     * 回调对象
     */
    private TestTextView.Callback mCallback1 = new TestTextView.Callback()
    {
        @Override
        public int getTextViewContent()
        {
            return 1;
        }
    };

    /**
     * 回调对象
     */
    private TestTextView.Callback mCallback2 = new TestTextView.Callback()
    {
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
        mCallback1.unregister(); //取消注册，在需要资源释放的地方要取消注册，否则会内存泄漏
        mCallback2.unregister();
    }
}
