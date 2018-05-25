# About
可以双向通信的流接口，用于Android开发中复杂嵌套，又需要双向通信的场景

# Gradle
`implementation 'com.fanwe.android:stream:1.0.0-rc1'`

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
private final TestFragment.FragmentCallback mFragmentCallback = new TestFragment.FragmentCallback()
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
