# About
可以双向通信的流接口，用于Android开发中复杂嵌套，又需要双向通信的场景<br>

实现原理：<br>
1. 利用java.lang.reflect.Proxy为接口生成一个代理对象
2. 监听代理对象方法被触发的时候，通知已经注册的对象

# 简单使用
1. 创建接口
```java
/**
 * Fragment中定义一个接口，接口继承流接口
 */
public interface FragmentCallback extends FStream
{
    void onClickFragment();

    String getActivityContent();
}
```

2. 创建接口代理对象
```java
private FragmentCallback mCallback = FStreamManager.getInstance().newProxyBuilder().build(FragmentCallback.class);
```

3. Fragment用代理对象通信
```java
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
             * 通知按钮点击
             */
            mCallback.onClickFragment(v);

            /**
             * 从Activity中获取内容
             */
            final String activityContent = mCallback.getActivityContent();
            button.setText(activityContent);
        }
    });
    return button;
}
```

4. Activity中注册流对象来和代理对象通信
```java
private TestFragment.FragmentCallback mFragmentCallback = new TestFragment.FragmentCallback()
{
    @Override
    public void onClickFragment(View v)
    {
        Toast.makeText(MainActivity.this, "onClickFragment", Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getActivityContent()
    {
        return "MainActivity";
    }

    @Override
    public Object getTag()
    {
        return null;
    }
};

/**
 * 注册回调对象
 */
FStreamManager.getInstance().register(mFragmentCallback);
/**
 * 取消注册，在需要资源释放的地方要取消注册，否则会内存泄漏
 */
FStreamManager.getInstance().unregister(mFragmentCallback);
```

# 注意
* 有多个代理对象的情况 <br> <br>
创建代理对象的时候可以指定tag，默认代理对象的tag是null。
只有流对象getTag()方法返回的值和代理对象tag相等的流对象才可以互相通信，tag比较相等的规则为 “==” 或者 “equals”，
流对象可以通过getTag()方法的返回值决定要和哪些代理对象通信，默认返回null <br> <br>

```java
private final FragmentCallback mCallback = FStreamManager.getInstance().newProxyBuilder()
        .tag(this) // 为代理对象设置一个tag
        .build(FragmentCallback.class);
```

* 有多个流对象的情况 <br> <br>
这如果调用代理对象通信的方法有返回值的话，默认是用最后注册的一个流对象方法的返回值，
当然，代理对象也可以在创建的时候设置一个方法返回值筛选器，筛选自己需要的返回值 <br> <br>

```java
private final FragmentCallback mCallback = FStreamManager.getInstance().newProxyBuilder()
        .methodResultFilter(new MethodResultFilter() // 为代理对象设置方法返回值筛选器
        {
            @Override
            public Object filterResult(Method method, Object[] args, List<Object> results)
            {
                // 筛选results中需要的返回值
                return null;
            }
        })
        .build(FragmentCallback.class);
```

```java
/**
 * 方法返回值过滤接口
 */
public interface MethodResultFilter
{
    /**
     * 筛选方法的返回值
     *
     * @param method  被触发的方法
     * @param args    被触发的方法的参数
     * @param results 所有注册的FStream对象该方法的返回值集合
     * @return 返回该方法最终的返回值，默认把返回值集合的最后一个当做该方法的返回值
     */
    Object filterResult(Method method, Object[] args, List<Object> results);
}

```

# 调试模式
```java
// 打开调试模式
FStreamManager.getInstance().setDebug(true);
```

调试模式打开后，会有类似以下的日志，方便调试

```java
// 注册流对象，流对象所属的接口class，流对象返回的tag，注册后这种class类型的流对象有几个
register:com.fanwe.stream.MainActivity$1@53810f5 class:com.fanwe.stream.TestFragment$FragmentCallback tag:null count:1

// 代理对象的方法被调用，调用的是哪个方法，代理对象的tag，这个接口下有几个流对象需要通知
notify -----> public abstract java.lang.String com.fanwe.stream.TestFragment$FragmentCallback.getActivityContent()  tag:null count:1

// 通知到了第几个流对象，它的返回值是什么
notify index:1 stream:com.fanwe.stream.MainActivity$1@53810f5 return:MainActivity

// 代理对象的方法执行后，最终的返回值是什么
notify final return:MainActivity

// 流对象取消注册
unregister:com.fanwe.stream.MainActivity$1@53810f5 class:com.fanwe.stream.TestFragment$FragmentCallback tag:null count:0

```
