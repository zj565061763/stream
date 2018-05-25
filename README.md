# Gradle
`implementation 'com.fanwe.android:stream:1.0.0-rc1'`

# 使用
做一个简单的demo，TextView点击之后，通过回调对象返回字符串设置给TextView
<br>
1. 自定义一个TextView
```java
public class TestTextView extends AppCompatTextView
{
    /**
     * 回调代理对象
     */
    private Callback mCallback = FStreamManager.getInstance().newProxyBuilder()
            /**
             * 设置代理对象的tag，默认tag为null
             * 注意：只有tag和当前代理对象tag相等的流对象才会被通知到，tag比较相等的规则为 “==” 或者 “equals”
             */
            .tag(null)
            /**
             * 设置方法返回值过滤对象，默认为null，会用最后一个注册的流对象的返回值
             */
            .methodResultFilter(null)
            .build(Callback.class);

    public TestTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                /**
                 * 调用回调代理对象的方法，从注册的流对象中得到一个返回值
                 */
                final int result = mCallback.getTextViewContent();
                setText(String.valueOf(result));
            }
        });
    }

    /**
     * 回调接口继承流接口
     */
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
```
