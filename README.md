# About
适用于Android开发中复杂嵌套，又需要双向通信的场景<br>

实现原理：<br>
1. 利用java.lang.reflect.Proxy为接口生成一个代理对象
2. 监听代理对象方法被触发的时候，通知已经注册的对象

# 项目Module需要支持java8
```
android {

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

}
```

# Gradle
[![](https://jitpack.io/v/zj565061763/stream.svg)](https://jitpack.io/#zj565061763/stream)

# 简单使用
1. 创建Fragment
```java
public class TestFragment extends Fragment
{
    /**
     * 创建接口代理对象
     */
    private final FragmentCallback mCallback = new FStream.ProxyBuilder().build(FragmentCallback.class);

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
```

2. Activity中注册流对象和代理对象通信
```java
public class MainActivity extends AppCompatActivity implements TestFragment.FragmentCallback
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * 添加TestFragment
         */
        getSupportFragmentManager().beginTransaction().add(R.id.framelayout, new TestFragment()).commit();
        /**
         * 注册回调对象
         */
        FStreamManager.getInstance().register(this);

    }

    @Override
    public String getActivityContent()
    {
        return "MainActivity";
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        /**
         * 取消注册
         * 不取消注册的话，流对象会一直被持有，此时流对象又持有其他UI资源对象的话，会内存泄漏
         */
        FStreamManager.getInstance().unregister(this);
    }
}
```

![](http://thumbsnap.com/i/ohw30sWe.gif?0527)

# 注意
* 有多个代理对象的情况 <br> <br>
创建代理对象的时候可以指定tag，默认代理对象的tag是null<br>
只有流对象getTagForClass()方法返回值和代理对象tag相等的时候，他们才可以互相通信，tag比较相等的规则为 “==” 或者 “equals”<br>
流对象可以重写getTagForClass()方法提供一个tag来决定要和哪些代理对象通信，默认返回null <br> <br>

```java
private final FragmentCallback mCallback = new FStream.ProxyBuilder()
        // 为代理对象设置一个tag
        .setTag(this)
        .build(FragmentCallback.class);
```

* 有多个流对象的情况 <br> <br>
这如果调用代理对象通信的方法有返回值的话，默认是用最后注册的一个流对象方法的返回值，
当然，代理对象也可以在创建的时候设置一个分发回调，筛选自己需要的返回值 <br> <br>

```java
private final FragmentCallback mCallback = new FStream.ProxyBuilder()
        // 为代理对象设置分发回调
        .setDispatchCallback(new DispatchCallback()
        {
            @Override
            public boolean onDispatch(Method method, Object[] methodParams, Object methodResult, Object observer)
            {
                return false;
            }
        })
        .build(FragmentCallback.class);
```

```java
/**
 * 流对象方法分发回调
 */
public interface DispatchCallback
{
    /**
     * 流对象的方法被通知的时候回调
     *
     * @param method       方法
     * @param methodParams 方法参数
     * @param methodResult 方法返回值
     * @param observer     流对象
     * @return true-停止分发，false-继续分发
     */
    boolean onDispatch(Method method, Object[] methodParams, Object methodResult, Object observer);
}
```

# 调试模式
```java
// 打开调试模式
FStreamManager.getInstance().setDebug(true);
```

调试模式打开后，会有类似以下的日志，日志的过滤tag：FStream

```java
// 注册流对象，流对象所属的接口class，流对象返回的tag，注册后这种class类型的流对象有几个
register:com.sd.stream.MainActivity$1@53810f5 class:com.sd.stream.TestFragment$FragmentCallback count:1

// 代理对象的方法被调用，调用的是哪个方法，代理对象的tag，这个接口下有几个流对象需要通知
notify -----> public abstract java.lang.String com.sd.stream.TestFragment$FragmentCallback.getActivityContent()  tag:null count:1

// 通知到了第几个流对象，它的返回值是什么
notify index:0 stream:com.sd.stream.MainActivity$1@53810f5 return:MainActivity

// 代理对象的方法执行后，最终的返回值是什么
notify final return:MainActivity

// 流对象取消注册
unregister:com.sd.stream.MainActivity$1@53810f5 class:com.sd.stream.TestFragment$FragmentCallback count:0

```
