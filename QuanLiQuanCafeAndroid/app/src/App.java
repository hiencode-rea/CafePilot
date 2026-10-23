package com.example.nlcsn2;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

// Ép toàn bộ ứng dụng dùng giao diện tối (giống phong cách GitHub dark)
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
