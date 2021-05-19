package com.sd.demo.stream;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.sd.lib.stream.FStream;
import com.sd.lib.stream.FStreamManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 添加TestFragment
        getSupportFragmentManager().beginTransaction().add(R.id.framelayout, new TestFragment()).commit();

        // 注册流对象
        FStreamManager.getInstance().register(mCallback1);

        // 注册流对象，并设置优先级，数值越大越先被通知，默认优先级：0
        FStreamManager.getInstance().register(mCallback2).setPriority(-1);

        // 绑定流对象，绑定之后会自动取消注册
        FStreamManager.getInstance().bindStream(mCallback3, this);
    }

    private final TestFragment.FragmentCallback mCallback1 = new TestFragment.FragmentCallback() {
        @Override
        public String getDisplayContent() {
            return "1";
        }

        @Nullable
        @Override
        public Object getTagForStream(@NonNull Class<? extends FStream> clazz) {
            return null;
        }
    };

    private final TestFragment.FragmentCallback mCallback2 = new TestFragment.FragmentCallback() {
        @Override
        public String getDisplayContent() {
            return "2";
        }

        @Nullable
        @Override
        public Object getTagForStream(@NonNull Class<? extends FStream> clazz) {
            return null;
        }
    };

    private final TestFragment.FragmentCallback mCallback3 = new TestFragment.FragmentCallback() {
        @Override
        public String getDisplayContent() {
            return "3";
        }

        @Nullable
        @Override
        public Object getTagForStream(@NonNull Class<? extends FStream> clazz) {
            return null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 要取消注册，否者流对象会一直被持有。
        FStreamManager.getInstance().unregister(mCallback1);
        FStreamManager.getInstance().unregister(mCallback2);
    }
}