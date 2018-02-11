## Gradle
[![](https://jitpack.io/v/zj565061763/stream.svg)](https://jitpack.io/#zj565061763/stream)

## 使用
做一个简单的demo，TextView点击之后，通过回调对象返回字符串设置给TextView
<br>
1. 自定义一个TextView
```java
public class TestTextView extends AppCompatTextView
{
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
```

2. 在Activity中创建并注册回调对象
```java
public class MainActivity extends AppCompatActivity
{
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
            /**
             * 如果调用此方法，则会用当前方法的返回值，如果不调用则用最后一个注册的callback的返回值
             */
            getNotifySession().requestAsResult(this);
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
```
