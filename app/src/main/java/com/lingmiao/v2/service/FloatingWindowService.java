package com.lingmiao.v2.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.lingmiao.v2.R;

public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 加载我们的 control_panel.xml 布局
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.control_panel, null);

        // 设置悬浮窗参数
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START; // 默认左上角
        params.x = 100;
        params.y = 100;

        // 将面板添加到屏幕
        windowManager.addView(floatingView, params);

        // ====== 1. 实现面板任意拖拽 ======
        View rootPanel = floatingView.findViewById(R.id.root_panel);
        rootPanel.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        // ====== 2. 实现右下角拖拽缩放 ======
        View resizeHandle = floatingView.findViewById(R.id.resize_handle);
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialWidth, initialHeight;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialWidth = floatingView.getWidth();
                        initialHeight = floatingView.getHeight();
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int newWidth = initialWidth + (int) (event.getRawX() - initialTouchX);
                        int newHeight = initialHeight + (int) (event.getRawY() - initialTouchY);
                        // 设置最小宽高限制，防缩太小
                        if (newWidth > 200 && newHeight > 300) {
                            params.width = newWidth;
                            params.height = newHeight;
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;
                }
                return false;
            }
        });

        // ====== 3. 实现顶部工具栏折叠/展开 ======
        View topToolbar = floatingView.findViewById(R.id.top_toolbar);
        TextView toggleBtn = floatingView.findViewById(R.id.btn_toggle_toolbar);
        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (topToolbar.getVisibility() == View.VISIBLE) {
                    topToolbar.setVisibility(View.GONE);
                    toggleBtn.setText("▶");
                } else {
                    topToolbar.setVisibility(View.VISIBLE);
                    toggleBtn.setText("◀");
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
