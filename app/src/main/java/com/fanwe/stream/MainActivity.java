package com.fanwe.stream;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.fanwe.lib.stream.FStreamManager;

public class MainActivity extends AppCompatActivity
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
        FStreamManager.getInstance().register(mFragmentCallback);
    }

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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        /**
         * 取消注册，在需要资源释放的地方要取消注册，否则会内存泄漏
         */
        FStreamManager.getInstance().unregister(mFragmentCallback);
    }
}
