package com.lingmiao.v2.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NestedScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lingmiao.v2.R;
import com.lingmiao.v2.service.FloatingWindowService;

public class MainActivity extends AppCompatActivity {

    private LinearLayout splashScreen, disclaimerScreen, homeScreen, choiceScreen;
    private NestedScrollView scrollContent;
    private TextView floatTip;
    private Button btnContact, btnEnter;
    private boolean isUnlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        startSplashAndAnimations();
        setupListeners();
    }

    private void initViews() {
        splashScreen = findViewById(R.id.splash_screen);
        disclaimerScreen = findViewById(R.id.disclaimer_screen);
        homeScreen = findViewById(R.id.home_screen);
        choiceScreen = findViewById(R.id.choice_screen);
        scrollContent = findViewById(R.id.scroll_content);
        floatTip = findViewById(R.id.float_tip);
        btnContact = findViewById(R.id.btn_contact);
        btnEnter = findViewById(R.id.btn_enter);
    }

    private void startSplashAndAnimations() {
        // 1. 启动页停留 10 秒
        new Handler().postDelayed(() -> {
            splashScreen.setVisibility(View.GONE);
            disclaimerScreen.setVisibility(View.VISIBLE);
            startFloatTextAnimation();
        }, 10000);

        // 2. 滑动到底部解锁
        scrollContent.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            View view = v.getChildAt(v.getChildCount() - 1);
            int diff = (view.getBottom() - (v.getHeight() + v.getScrollY()));
            if (diff <= 10) {
                if (!isUnlocked) {
                    isUnlocked = true;
                    btnEnter.setEnabled(true);
                    btnEnter.setText("确定并进入");
                    btnEnter.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_purple));
                    btnEnter.setTextColor(getResources().getColor(android.R.color.white));
                    floatTip.setVisibility(View.GONE);
                }
            }
        });
    }

    private void startFloatTextAnimation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(floatTip, "translationY", 0f, -6f, 0f);
        animator.setDuration(1800);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.start();
    }

    private void setupListeners() {
        btnContact.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "开发者邮箱：developer@example.com", Toast.LENGTH_SHORT).show();
        });

        btnEnter.setOnClickListener(v -> {
            if (!isUnlocked) return;
            disclaimerScreen.setVisibility(View.GONE);
            homeScreen.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btn_wake).setOnClickListener(v -> {
            homeScreen.setVisibility(View.GONE);
            choiceScreen.setVisibility(View.VISIBLE);
        });

        // ====== 核心：点击横竖屏，启动悬浮窗 ======
        findViewById(R.id.btn_horizontal).setOnClickListener(v -> startFloatingWindow());
        findViewById(R.id.btn_vertical).setOnClickListener(v -> startFloatingWindow());
    }

    // 启动悬浮窗（自动检查是否有权限）
    private void startFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // 没有权限，引导去设置开启
                Toast.makeText(this, "请授予屏幕悬浮窗权限以启用辅助功能", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }
        }
        // 有权限，启动悬浮窗服务
        Intent intent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        // 提示用户
        Toast.makeText(this, "粉色星轨控制窗已弹出！", Toast.LENGTH_SHORT).show();
    }
}
