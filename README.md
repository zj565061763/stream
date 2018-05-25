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

# 调试模式

设置调试模式后，内部会输出流对象注册和取消注册，已经代理对象方法触发的日志，demo中的日志如下

```java

// 流对象注册，注册时候的tag，注册后这种类型的流对象的个数
register:com.fanwe.stream.MainActivity$1@8cf27e6 tag(null) 1
register:com.fanwe.stream.MainActivity$2@404127 tag(null) 2

// 流代理对象的方法被调用，代理对象的tag
notify -----> public abstract int com.fanwe.stream.TestTextView$Callback.getTextViewContent()  tag(null)

// 通知第几个流对象，这个流对象的返回值是什么
notify index:1 stream:com.fanwe.stream.MainActivity$1@8cf27e6 return:1
notify index:2 stream:com.fanwe.stream.MainActivity$2@404127 return:2

// 最终的返回值是什么
notify final return 2

// 取消注册流对象，取消注册的这个流对象的tag，取消注册后剩余的相同类型的流对象的个数
unregister:com.fanwe.stream.MainActivity$1@8cf27e6 tag(null) 1
unregister:com.fanwe.stream.MainActivity$2@404127 tag(null) 0
```

```java
FStreamManager.getInstance().setDebug(true);
```
