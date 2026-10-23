package com.example.nlcsn2;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

// Giữ giao diện theo theme của ứng dụng để màu sắc hiển thị nhất quán trên mobile.
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
