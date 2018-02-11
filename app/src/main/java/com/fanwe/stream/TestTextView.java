package com.fanwe.stream;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;

import com.fanwe.lib.stream.FStream;
import com.fanwe.lib.stream.FStreamManager;

/**
 * Created by Administrator on 2018/2/9.
 */
public class TestTextView extends AppCompatTextView
{
    public static final String TAG = TestTextView.class.getSimpleName();

    /**
     * 回调代理对象
     */
    private Callback mCallback = FStreamManager.getInstance().newPublisher(Callback.class);

    public TestTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setText(String.valueOf(mCallback.getTextViewContent()));
            }
        });
    }

    public interface Callback extends FStream
    {
        int getTextViewContent();
    }
}
